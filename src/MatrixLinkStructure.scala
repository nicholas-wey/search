package search.src

import scala.collection.mutable.Set
import scala.collection.mutable.HashSet

/**
 * A very inefficient implementation of LinkStructure. The IDs of the
 * documents are assumed to be integers from 0 to the length of the
 * given matrix
 *
 * @param linksMatrix A square matrix such that some cell (i, j) is
 *   true iff document i links to j
 */
class MatrixLinkStructure(linksMatrix: Array[Array[Boolean]]) extends LinkStructure[Int] {

  override def ids: Iterable[Int] = {
    val ids = new Array[Int](linksMatrix.length)
    for (i <- 0 until linksMatrix.length) {
      ids(i) = i
    }
    ids
  }

  override def links(id: Int): Set[Int] = {
    val links = new HashSet[Int]
    for (i <- 0 until linksMatrix.length) {
      if (linksMatrix(id)(i)) {
        links += i
      }
    }
    links
  }
}
