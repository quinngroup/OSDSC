# OSDSC - Open Source Distributed Spectral Clustering


OSDSC - Open Source Distributed Spectral Clustering is a distributed rendition of the Spectral Clustering algorithm.
## Building
```
sbt "set test in assembly := {}" clean assembly
```
## Prerequisites
You should have [Apache Spark](http://spark.apache.org/) of version 1.6.2 installed in your cluster. Refer to Apache Spark's [documents](http://spark.apache.org/docs/1.6.2/) for details.
You should have [SBT](http://www.scala-sbt.org/index.html) of version 0.13.8 installed on your master. Refer to SBT [documents](http://www.scala-sbt.org/documentation.html) for details.
You should have [Scala](https://www.scala-lang.org/) of version 2.10.5 installed on your master. Refer to Scala [documents](http://docs.scala-lang.org/) for details.
## Use OSDSC with Spark
1. Build OSDSC, `sbt package` and find `osdsc-<version>.jar` in `target/`
2. Deploy `osdsc-<version>.jar` to master machine.
3. Run spark by `bin/spark-submit`

## Input Parameters

The input given is in the following sequence:

1. Input File: Dataset of comma separated values
2. Number of dimensions: Number of dimensions of the dataset
3. Number of points: Number of data points.
4. Number of neighbors: Number of neighbors to consider for sparse affinity matrix construction.
5. Gamma: Gamma value for the Gaussian kernel
6. Number of clusters: Number of clusters in the dataset.
7. Number of iterations: Number of KMeans iterations to perform.

## Example

Run the example by:

```
bin/spark-submit --class "ann" target/osdsc-<version>.jar iris.txt 4 150 10 0.01 3 20
```

## Features

We present a distributed and open source version for the Spectral Clustering algorithm.
The design of the algorithm reduces the computational time complexity and space complexity of the algorithm by approximating the similarity matrix construction and also by using the Apache Spark framework.
The experimental results show that the proposed method also maintains the clustering accuracy.


## Credits

[ANNOY](https://github.com/spotify/annoy) used for computing the approximate nearest neighbors.

Ulrike von Luxburg. A tutorial on spectral clustering. U. Stat Comput (2007) 17: 395. https://doi.org/10.1007/s11222-007-9033-z

## Whom to Contact?

Feel free to post any questions or comments in the Issues section.
In case of any questions you can also mail Ankita Joshi at ankita.joshi25@uga.edu

