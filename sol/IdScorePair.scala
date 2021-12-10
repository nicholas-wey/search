package search.sol

/**
 * a class that holds the id and computed document score after a query for any
 * one document
 *
 * @param idNum - an Int representing the id of a document
 * @param docScore - a Double representing the computed document score after a
 * query for any one document
 */
class IdScorePair(idNum: Int, docScore: Double) {

  val id = idNum
  val finalScore = docScore

}