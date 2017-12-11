package annoy4s

import scala.collection.mutable.ArrayBuffer

object Angular extends Metric with AngularSerde {

  import Functions._

  override val name = "angular"

  override def distance(x: Array[Float], y: Array[Float]): Float = {
    require(x.length == y.length)
    val pp = blas.dot(x, x)
    val qq = blas.dot(y, y)
    val pq = blas.dot(x, y)
    val ppqq: Double = pp * qq
    if (ppqq > 0) (2.0 - 2.0 * pq / Math.sqrt(ppqq)).toFloat else 2.0f
  }

  override def margin(n: Node, y: Array[Float], buffer: Array[Float]): Float = blas.dot(n.getVector(buffer), y)

  override def side(n: Node, y: Array[Float], random: Random, buffer: Array[Float]): Boolean = {
    val dot = margin(n, y, buffer)
    if (dot != Zero) {
      dot > 0
    } else {
      random.flip()
    }
  }

  override def createSplit(nodes: ArrayBuffer[Node], dim: Int, rand: Random, n: Node): Unit = {
    val bestIv = new Array[Float](dim)
    val bestJv = new Array[Float](dim)
    twoMeans(nodes, true, bestIv, bestJv, this, rand)

    val vectorBuffer = n.getVector(new Array[Float](dim))
    var z = 0
    while (z < dim) {
      vectorBuffer(z) = bestIv(z) - bestJv(z)
      z += 1
    }
    normalize(vectorBuffer)
    n.setV(vectorBuffer)
  }

  override def normalizeDistance(distance: Float): Float = {
    math.sqrt(math.max(distance, Zero)).toFloat
  }
}
