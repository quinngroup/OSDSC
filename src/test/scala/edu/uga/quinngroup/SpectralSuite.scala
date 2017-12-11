package edu.uga.quinngroup


import org.apache.spark.SparkFunSuite
import org.apache.spark.mllib.linalg.{DenseVector, SparseVector, Vector, Vectors}
import org.apache.spark.mllib.util.{LocalClusterSparkContext, MLlibTestSparkContext}
import org.apache.spark.mllib.util.TestingUtils._
import org.apache.spark.util.Utils

class SpectralSuite extends SparkFunSuite// with MLlibTestSparkContext {
{
 

  test("single cluster") {
   

	val file = "iris.txt"//"/home/ankita/datasets/2048.csv"
	val dimension = 4//args(1).toInt //2
	val numberofPoints = 150 //args(2).toInt //100
	val neighbors = 3 //args(3).toInt
	val gamma = 0.01//args(4).toDouble
	val numberOfClusters = 3 // args(5).toInt
	val numIterations = 20//args(6).toInt
	val initRDD = sc.textFile(file).persist(MEMORY_AND_DISK)

	val rdd = initRDD.map{
		points  =>
		    val tokens = points.split(",")
		    //val label = tokens(0).toInt
		    val vec = tokens.slice(0,(dimension-1)).map(_.toFloat)
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

