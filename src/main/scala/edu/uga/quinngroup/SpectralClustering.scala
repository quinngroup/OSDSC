/** Implementation of Spectral Clustering
  *
  * Work done by Ankita Joshi under the guidance of Dr. Shannon Quinn
  * License information
  */
package edu.uga.quinngroup


import annoy4s.spark.AnnoyModel
import annoy4s.spark.Annoy
import org.apache.spark.sql.{Row, DataFrame, SQLContext}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel._


class SpectralClustering private(
  private var g: Double, //gamma
  private var numClusters: Int, //number of clusters to input to kmeans
  private var numIterations: Int) {

  /**
   * Constructs a SpectralClustering instance with default parameters: { g: 0.001, numClusters: 2, numIterations: 20, inputpath: current directory, output: current directory,
   * groundtruth file: blank.
   */
  def this() = this(0.001, 2, 20)

  /*
  * The gamma to choose (g).
  */
  def getGamma: Double = g

  /*
   * Set the gamma (g). Default: 0.001
   */
  def setGamma(g: Double): this.type = {
    this.g = g
    this
  }

  def getNumClusters: Int = numClusters

  def setNumClusters(numClusters: Int): this.type = {
    require(numClusters > 0, s"Number of clusters must be positive but got ${numClusters}")
    this.numClusters = numClusters
    this
  }

  /*
   * The maximum number of iteration to run for KMeans
   */
  def getIterations: Int = numIterations

  /*
   * Set the maximum number of iteration to run for KMeans. Default: 20
   */
  def setIterations(numIterations: Int): this.type = {

    require(numIterations > 0, s"Number of iterations must be positive but got ${numIterations}")
    this.numIterations = numIterations
    this
  }


  /**
    * Calculates affinity matrix of the input file given. Affinity matrix is the matrix which gives the similarity between any two points in the data.
    */
  def calculateAffinity(B: RDD[(org.apache.spark.mllib.linalg.Vector, Long)], A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)], g: Double): RDD[(Long, (Long, Double))] = {

    /*
     * Take cartesian product.
     */

    val cProduct = B.cartesian(A)

    //rbf kernel used to find affinity rbf(x,y) = exp(-gamma * (squared distance between x,y)
    val affinity = cProduct.map({ case ((vector1, index1), (vector2, index2)) => (index1, (index2, Math.exp(-g * Vectors.sqdist(vector1, vector2)))) })
    affinity
  }

  /*
   * Calculates the normalized affinity as W = D^-1/2 A D^-1/2
   */
  def laplacian(
      finalAffinity: RDD[IndexedRow],
      diagonalMatrix: Broadcast[org.apache.spark.mllib.linalg.Vector]): IndexedRowMatrix = {

    val W = new IndexedRowMatrix(finalAffinity.map(row => {

      val i = row.index.toInt
      val w = row.vector.toArray
      val d = diagonalMatrix.value.toArray
      for (point <- 0 until w.length) {
        w(point) *= d(i) * d(point)
      }
      new IndexedRow(i, Vectors.dense(w))
    }))

    W


  }

def laplacian2(
      finalAffinity: RDD[IndexedRow],
      diagonalMatrix: Broadcast[org.apache.spark.mllib.linalg.Vector]): IndexedRowMatrix = {

      val W = new IndexedRowMatrix(finalAffinity.map(row => {

      val i = row.index.toInt

      val len = row.vector.toArray.length

      val spvector = row.vector.toSparse
      //val vectorvalues = row.vector.toArray

      val a = spvector.values
      val k = spvector.indices
      val d = diagonalMatrix.value.toArray
     for (point <- k; g <- 0 until a.length) {

      a(g) *= d(i) * d(point)
      }
      
      new IndexedRow(i, Vectors.sparse(len,k,a))
    }))
  W 
}
  /*
   * KMeans implementation to caculate the clusters.
   */
  def doKMeans(
      urdd: org.apache.spark.rdd.RDD[org.apache.spark.mllib.linalg.Vector],
      numClusters: Integer,
      numIterations: Integer): KMeansModel = {

    val clusters = KMeans.train(urdd, numClusters, numIterations)
    clusters
  }

  def test(
      A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],
      B: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],
      g: Double, transformed: RDD[IndexedRow],
      scObject: KMeansModel): RDD[(Long, Int)] = {

    val indexval = A.count()
    val newB = B.map { case (x, index) => ((index + indexval, "test"), x) }

    val newA = A.map(_.swap)

    //affinity of B with A
    val M = newB.cartesian(newA).map {
      case ((index1, vector1), (index2, vector2)) => (index1, (index2, vector1, vector2))
    }.filter { case (k1, (k2, v1, v2)) =>

      k1._2 == "test"

    }.map {
      case (k, (i2, v1, v2)) =>
        (k._1, Math.exp(-g * Vectors.sqdist(v1, v2)))
    }.groupByKey()

    //take u transpose
    val t = transformed.map(_.vector).map { case (values) => values.toArray }.flatMap(x => x.zip(Stream from 1)).map(x => x.swap).groupByKey().map(x => new IndexedRow(x._1, Vectors.dense(x._2.toArray)))

    //Take projections of the new affinity matrix onto transposed matrix of U
    val projections = M.map { case (k, v) => ((k, "M"), v) }.cartesian(t.map(x => (x.index, x.vector))).map {
      case ((index1, vector1), (index2, vector2)) => (index1, (index2, vector1, vector2))
    }.filter {
      case (k1, (k2, v1, v2)) =>
        k1._2 == "M"
    }.map {
      case (k, v) =>

        val s = v._2.toArray.length
        val s1 = v._2.toArray
        val s2 = v._2.toArray
        var ans = 0.0
        var i = 0
        while (i < s) {
          ans += s1(i) * s2(i)
          i += 1
        }

        (k._1, ans)
    }.groupByKey().map(x => new IndexedRow(x._1, Vectors.dense(x._2.toArray)))


    val predictions = projections.map(row => (row.index, scObject.predict(row.vector)))

    predictions
  }


  private def runAlgorithm(A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)]): (org.apache.spark.mllib.clustering.KMeansModel, org.apache.spark.rdd.RDD[org.apache.spark.mllib.linalg.distributed.IndexedRow]) = {

    val sc = A.sparkContext

    val initializationMode = "dense"

    //calculate affinity of all points. For points ([1,(x1,y1)],[2,(x2,y2)]) => (1,(2,affinityValue))
    val affinity = calculateAffinity(A, A, g)

    //Affinity of one point with all other points. (x, ((1,affinityValue)..(x,affinityValue)..(n,affinityValue)))
    val allAffinity = affinity.groupByKey()

    //remove the second index, and make an IndexedRow => [index1 ,(affinity1 , ...., affinityn)]
    val finalAffinity = allAffinity.map({ case (index, affinities) => new IndexedRow(index, Vectors.dense(affinities.toSeq.map(_._2).toArray)) })

    //build a diagonal matrix
    val diagonalMatrix = sc.broadcast(Vectors.dense(finalAffinity.map(v => 1 / Math.sqrt(v.vector.toArray.sum)).collect()))

    //Calculate the normalized Affinity.    
    val W = laplacian(finalAffinity, diagonalMatrix)

    //compute the SVD
    val svd = W.computeSVD(numClusters, computeU = true)
    val u = svd.U.rows
    // val urdd = u.map(_.vector)
    val normalizer1 = new Normalizer()
    //   val transformed = normalizer1.transform(urdd)
    val transformed = u.map(row => new IndexedRow(row.index, normalizer1.transform(row.vector)))
    //KMeans step
    //val clusters = doKMeans(transformed.map(_.vector), numClusters,numIterations)
    val clusters = doKMeans(transformed.map(_.vector), numClusters, numIterations)


    (clusters, transformed)

  }


  def runKNN(
  rddn: RDD[(Int,Array[Float])],
  dimension: Int, 
  numPoints: Int, 
  neighbors: Int,
  g: Double,
  numClusters: Int
      ): (KMeansModel, RDD[IndexedRow]) = {


    //val sc = A.sparkContext

    val initializationMode = "sparse"
    val idCol = "id"
    val featuresCol = "features"
    val outputCol = "output"

    val sc = rddn.sparkContext

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    
     val dataset: DataFrame = rddn.toDF("id", "features")

  //  dataset.show()

    val annoyModel: AnnoyModel = new Annoy()
      .setDimension(dimension-1)
      .setIdCol(idCol)
      .setFeaturesCol(featuresCol)
      .setOutputCol(outputCol)
      .setDebug(true)
//  .setMetric("euclidean")
      .fit(dataset)

    val result: DataFrame = annoyModel
      .setK(neighbors) // find 10 neighbors
      .transform(dataset)

//    result.show()


//val res = result.rdd.map(row => (row(0), Vectors.dense((row.getAs[Seq[Float]](1).toArray).map(_.toDouble)),row(2)))
//toArray removed in the end
val res2 = result.rdd.map{case(row) =>(row.getInt(0) , Vectors.dense((row.getAs[Seq[Float]](1).toArray).map(_.toDouble)),row.getAs[List[Int]](2).toArray )}

//val r = res2.map{r => r._3.map(nr => (nr,(r._1,r._2)))}
val r = res2.map{case(k1,k2,k3) => k3.map(nr => (nr,(k1,k2)))}

val s = r.flatMap(x => x)


val affinity = s.join(rddn).map{case( (index2, ((index1, point1),point2)) ) =>  

val p2 = Vectors.dense(point2.map(_.toDouble))
val d = Math.exp(-g * Vectors.sqdist(p2, point1))
((index1,index2),d)

}


val finalAffinity =affinity.map({ case ((index1, index2), affinities) => (index1, (index2, affinities)) }).groupByKey().map({ case (index, values) => new IndexedRow(index, Vectors.sparse(numPoints, values.toSeq.map(_._1.toInt).toArray, values.toSeq.map(_._2).toArray)) })



val diagonalMatrix = sc.broadcast(Vectors.dense(finalAffinity.map(v => 1 / Math.sqrt(v.vector.toArray.sum)).collect()))

val W = laplacian2(finalAffinity, diagonalMatrix)

val svd = W.computeSVD(numClusters, computeU = true)
val u = svd.U.rows
// val urdd = u.map(_.vector)
val normalizer1 = new Normalizer()
//   val transformed = normalizer1.transform(urdd)
val transformed = u.map(row => new IndexedRow(row.index, normalizer1.transform(row.vector)))

val res = transformed.map(_.vector)
val clusters = doKMeans(res, numClusters, numIterations)

(clusters, transformed)
  }
}

object SpectralClustering {


  def train(
      A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],
      g: Double,
      numClusters: Int,
      numIterations: Int): (KMeansModel, RDD[IndexedRow]) = {

    new SpectralClustering().setGamma(g)
      .setNumClusters(numClusters)
      .setIterations(numIterations)
      .runAlgorithm(A)

  }


  def train(
      A: RDD[(Int, Array[Float])],
      dimension: Int,
      numPoints: Int,
      neighbors: Int,
      g: Double,
      numClusters: Int,
      numIterations: Int): (KMeansModel, RDD[IndexedRow]) = {

    new SpectralClustering().setGamma(g)
      .setNumClusters(numClusters)
      .setIterations(numIterations)
      .runKNN(A, dimension, numPoints,neighbors,g,numClusters)

  }


  def predict(
      A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],
      B: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],
      g: Double,
      transformed: RDD[IndexedRow],
      scObject: KMeansModel): RDD[(Long, Int)] = {
    new SpectralClustering().test(A, B, g, transformed, scObject)
  }


}


    

