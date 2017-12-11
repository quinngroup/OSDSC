{
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "# OSDSC - Open Source Distributed Spectral Clustering\n",
    "\n",
    "\n",
    "OSDSC - Open Source Distributed Spectral Clustering is a distributed rendition of the Spectral Clustering algorithm.\n",
    "## Building\n",
    "```\n",
    "sbt \"set test in assembly := {}\" clean assembly\n",
    "```\n",
    "## Prerequisites\n",
    "You should have [Apache Spark](http://spark.apache.org/) of version 1.6.2 installed in your cluster. Refer to Apache Spark's [documents](http://spark.apache.org/docs/1.6.2/) for details.\n",
    "You should have [SBT](http://www.scala-sbt.org/index.html) of version 0.13.8 installed on your master. Refer to SBT [documents](http://www.scala-sbt.org/documentation.html) for details.\n",
    "You should have [Scala](https://www.scala-lang.org/) of version 2.10.5 installed on your master. Refer to Scala [documents](http://docs.scala-lang.org/) for details.\n",
    "## Use OSDSC with Spark\n",
    "1. Build OSDSC, `sbt package` and find `osdsc-<version>.jar` in `target/`\n",
    "2. Deploy `osdsc-<version>.jar` to master machine.\n",
    "3. Run spark by `bin/spark-submit`\n",
    "\n",
    "## Input Parameters\n",
    "\n",
    "The input given is in the following sequence:\n",
    "\n",
    "1. Input File: Dataset of comma separated values\n",
    "2. Number of dimensions: Number of dimensions of the dataset\n",
    "3. Number of points: Number of data points.\n",
    "4. Number of neighbors: Number of neighbors to consider for sparse affinity matrix construction.\n",
    "5. Gamma: Gamma value for the Gaussian kernel\n",
    "6. Number of clusters: Number of clusters in the dataset.\n",
    "7. Number of iterations: Number of KMeans iterations to perform.\n",
    "\n",
    "## Example\n",
    "\n",
    "Run the example by:\n",
    "\n",
    "```\n",
    "bin/spark-submit --class \"ann\" target/osdsc-<version>.jar iris.txt 4 150 10 0.01 3 20\n",
    "```\n",
    "\n",
    "## Features\n",
    "\n",
    "We present a distributed and open source version for the Spectral Clustering algorithm.\n",
    "The design of the algorithm reduces the computational time complexity and space complexity of the algorithm by approximating the similarity matrix construction and also by using the Apache Spark framework.\n",
    "The experimental results show that the proposed method also maintains the clustering accuracy.\n",
    "\n",
    "\n",
    "## Credits\n",
    "\n",
    "[ANNOY](https://github.com/spotify/annoy) used for computing the approximate nearest neighbors.\n",
    "\n",
    "Ulrike von Luxburg. A tutorial on spectral clustering. U. Stat Comput (2007) 17: 395. https://doi.org/10.1007/s11222-007-9033-z\n",
    "\n",
    "## Whom to Contact?\n",
    "\n",
    "Feel free to post any questions or comments in the Issues section.\n",
    "In case of any questions you can also mail Ankita Joshi at ankita.joshi25@uga.edu\n",
    "\n",
    "\n"
   ]
  },
  {
   "cell_type": "code",
   "execution_count": null,
   "metadata": {
    "collapsed": true
   },
   "outputs": [],
   "source": []
  }
 ],
 "metadata": {
  "kernelspec": {
   "display_name": "Python 2",
   "language": "python",
   "name": "python2"
  },
  "language_info": {
   "codemirror_mode": {
    "name": "ipython",
    "version": 2
   },
   "file_extension": ".py",
   "mimetype": "text/x-python",
   "name": "python",
   "nbconvert_exporter": "python",
   "pygments_lexer": "ipython2",
   "version": "2.7.11+"
  }
 },
 "nbformat": 4,
 "nbformat_minor": 0
}
