package edu.uga.quinngroup

import breeze.linalg._
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel._

object ClusteringUtils {
/*
   * Silhoutte validation of clusters
   * References
   * 1. Peter J. Rousseeuw (1987). "Silhouettes: a Graphical Aid to the
        Interpretation and Validation of Cluster Analysis". Computational
        and Applied Mathematics 20: 53-65. doi:10.1016/0377-0427(87)90125-7.
   * 2. http://en.wikipedia.org/wiki/Silhouette_(clustering)
   */
  def silhoutte(indexPointClass: RDD[(Int,(org.apache.spark.mllib.linalg.Vector,Int))]): Double={         //(A: RDD[(org.apache.spark.mllib.linalg.Vector, Long)],predictions: RDD[(Int, Iterable[Long])]) ={
   
    
    
    //cartesian all the points
    val allPoints = indexPointClass.cartesian(indexPointClass)
    
    //calculate euclidian distance and output: (index1,index2,class1,class2,euclidean distance)
    val distance = allPoints.map({ case((id1,(point1,class1)),(id2,(point2,class2))) => ((id1,class1,class2),(Vectors.sqdist(point1,point2),1))})
    //distance.foreach(println)
    
    //calculate average euclidean for all the points
    val alldistances = distance.reduceByKey({case ((d1,count1), (d2,count2)) =>  ((d1*count1+d2*count2)/(count1+count2), count1+count2)})
    
    //for each point (index,class) we get its distance in the same class and in all other classes. 
    val distances = alldistances.map({case((id1,class1,class2),(distance,count)) => ((id1,class1),(class2,distance))}).groupByKey() 
    
    val silhoutte = distances.map({case((id,c),iter) => { var a =0.0
                                          var b=Double.PositiveInfinity
                                          for( (ci,di) <- iter){
                                            if(c == ci)
                                              a = di
                                            else
                                              if(di < b)
                                                b = di
                                                
                                          }
                                          val s = (b-a)/max(a,b)
                                          ((id,c),s)                            
                             
                                          }
    } )
    //silhoutte.foreach(println)
    
    //overall silhoutte score. Average of all silhoutte scores
    val silhoutteScore = silhoutte.map({case((id,c),score) => (score,1)}).reduce({case ((score1,count1),(score2,count2)) =>((score1*count1+score2*count2)/(count1+count2),(count1+count2))})._1
    
    
    silhoutteScore
  }

  /*
   * Rand Index
   */
  def randIndex(indexWithClass: RDD[(Long,Int)], originalData: RDD[(Long,Int)]): Double ={
    
    val points = indexWithClass.join(originalData).persist(DISK_ONLY)
    val allPoints = points.cartesian(points).filter({case((id1,value1),(id2,value2)) => id1<id2})
    val aggregate = allPoints.map({ case ((id1,(c1,oc1)),(id2,(c2,oc2))) => 
                                    if(c1==c2 && oc1 == oc2){
                                        (1,0,0,0) // True Positive
                                    }else if(c1!=c2 && oc1!=oc2){
                                        (0,1,0,0)  // True Negative
                                    }else if(c1 ==c2 && oc1!=oc2){
                                        (0,0,1,0)  //False Positive
                                    }else{
                                        (0,0,0,1)  //False Negative
                                    }
                                  }).reduce({case ((tp1,tn1,fp1,fn1),(tp2,tn2,fp2,fn2)) => (tp1+tp2,tn1+tn2,fp1+fp2,fn1+fn2)})
                                  
    val randIndexScore = (aggregate._1 + aggregate._2).toDouble / (aggregate._1+aggregate._2+aggregate._3+aggregate._4).toDouble                             
    //println(aggregate)
    
    randIndexScore
  }

}

  /*
    silhoutte validation or randIndex evaluation of the clusters by
    (point,index) -> (index,point)

   
    val indexWithPoint = A.map(_.swap)   
    //(class , all indexes) to (index,class)    
    val indexWithClass = predictions.flatMap({case (cluster,indexes) => indexes.map( index => (index,cluster))})
    
    //join above two on index to get: (index,point,cluster)
    val indexPointClass = indexWithPoint.join(indexWithClass).map({case (a,b) => (a.toInt,b)})
    
    val silhoutteScore = silhoutte(indexPointClass)
        
    val groundtruth = sc.textFile(groundtruthFile).map(_.toInt).zipWithIndex.map(_.swap)
    val randIndexScore =randIndex(indexWithClass,groundtruth)
    
    println(silhoutteScore)
    println(randIndexScore)
    */
  
