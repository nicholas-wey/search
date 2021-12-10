package search.sol
import collection.mutable.HashMap

/**
 * a class which represents a page in the wiki corpus, with fields that hold
 * its title, pagerank, and term-frequency HashMap
 *
 * @param doctitle - a String representing the title of the document
 * @param pr - a Double representing the PageRank score of tge document
 * @tfHashMap - a HashMap[String, Double] representing the term-frequency
 * HashMap for that particular document
 */
class Page(doctitle: String, pr: Double, tfHashMap: HashMap[String, Double]) {
  val title = doctitle
  var pageRank = pr
  val tfMap = tfHashMap

  def setPageRank(pR: Double) {
    this.pageRank = pR
  }
}