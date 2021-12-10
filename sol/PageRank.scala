package search.sol

import search.src.Ranker
import search.src.LinkStructure
import search.src.Ranking
import search.src.ArrayRanking
import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.xml.Node
import scala.xml.NodeSeq

/**
 * a class which implements methods which compute the Page Rank of all the
 * documents in a wiki corpus based on their linking structure
 *
 * @param numPages - an Int representing the number of pages in the corpus
 * @param idSeq - a NodeSeq representing the sequence of all the id's in the
 * corpus
 */
class PageRank(numPages: Int, idSeq: NodeSeq) extends Ranker[Int] {

  /**
   * a method that computes the distance between the r and rPrime arrays by
   * calculating the Euclidean distance for each index i in r and each
   * corresponding index j in rPrime
   *
   * @param r - a double Array representing ranks on the ith iteration of
   * a loop ranking method
   * @param rPrime - a double Array representing ranks on the (i+1)th iteration
   * of a ranking method
   */
  private def distance(r: Array[Double], rPrime: Array[Double]): Double = {
    var sum = 0.0
    for (i <- 0 to r.length - 1) {
      sum = sum + Math.pow((rPrime(i) - r(i)), 2)
    }
    sum
  }

  override def computePageRank(linkStruct: LinkStructure[Int]): Ranking[Int] = {
    val r = new Array[Double](numPages)
    val rPrime = new Array[Double](numPages)
    for (i <- 0 to r.length - 1) {
      r(i) = 0.0
    }
    for (i <- 0 to rPrime.length - 1) {
      rPrime(i) = 1.0 / numPages
    }
    while (distance(r, rPrime) > Epsilon) {
      for (i <- 0 to numPages - 1) {
        r(i) = rPrime(i)
      }
      for (j <- 0 to numPages - 1) {
        rPrime(j) = 0
        for (k <- 0 to numPages - 1) {
          val idj = idSeq.apply(j)
          val idIntJ = idj.text.trim.toInt
          val idk = idSeq.apply(k)
          val idIntK = idk.text.trim.toInt
          val x = linkStruct.links(idIntK)
          if (x.isEmpty) {
            if (k == j) {
              rPrime(j) = rPrime(j) + r(k) * ((DampeningFactor / numPages))
            } else {
              rPrime(j) = rPrime(j) + r(k) * ((DampeningFactor / numPages) +
                ((1 - DampeningFactor) / (numPages - 1)))
            }
          } else {
            if (x.contains(idIntJ) && (k != j)) {
              rPrime(j) = rPrime(j) + r(k) * ((DampeningFactor / numPages) +
                ((1 - DampeningFactor) / x.size))
            } else {
              rPrime(j) = rPrime(j) + r(k) * ((DampeningFactor / numPages))
            }
          }
        }
      }
    }
    if (numPages == 1) {
      rPrime(0) = 1.0
    }
    val pageRanksMap = new HashMap[Int, Double]
    for (i <- 0 to rPrime.length - 1) {
      val id = idSeq.apply(i)
      val idInt = id.text.trim.toInt
      pageRanksMap.put(idInt, rPrime(i))
    }
    val rankings = new PageRanking(pageRanksMap)
    rankings
  }
}