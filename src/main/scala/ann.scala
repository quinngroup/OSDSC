import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel._
import java.io._
import edu.uga.quinngroup.SpectralClustering
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.linalg.distributed.{IndexedRow, IndexedRowMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import breeze.linalg._
import breeze.plot._
import java.awt.Color


object ann {

	def main(args: Array[String]): Unit ={


	val conf = new SparkConf().setAppName("ann")
    val sc = new SparkContext(conf)

	val file = args(0)//"/home/ankita/datasets/2048.csv"
  val dimension = args(1).toInt //2
  val numberofPoints = args(2).toInt //100
  val neighbors = args(3).toInt
  val gamma = args(4).toDouble
  val numberOfClusters = args(5).toInt
  val numIterations = args(6).toInt


  val initRDD = sc.textFile(file).persist(MEMORY_AND_DISK)

  val rdd = initRDD.map{
                points  =>
                    val tokens = points.split(",")
                    //val label = tokens(0).toInt
                    val vec = tokens.slice(0,(dimension-1)).map(_.toFloat)
                    //println(vec)
		    vec
            }.zipWithIndex.map{case(a,b) => (b.toInt,a)}

	val (scObject,urdd) = SpectralClustering.train(rdd,dimension,numberofPoints,neighbors,gamma,numberOfClusters,numIterations)

  val predictions = urdd.map( row => (scObject.predict(row.vector),row.index)).groupByKey()




//plotting stuff
 var f = Figure()
     var p = f.subplot(0)

    val points = rdd.collectAsMap() 
    val indexes = predictions.collect()

    val colors = Array(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN)

    val q =  indexes.flatMap({ case (clusterID,indexToPoints) => indexToPoints.map( index =>{ val pt =points(index.toInt); (pt(0),pt(1),clusterID)})})
    p += scatter(q.map(_._1),q.map(_._2),_=>0.25, i => colors(q(i)._3))
    
    p.xlabel="X-Axis"
    p.ylabel="Y-Axis"
f.saveas("graph1.png")






}
}
 
