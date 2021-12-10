package search.sol

import search.src.Ranking
import scala.collection.mutable.HashMap

/**
 * a class that represents our final PageRankings object that extends the
 * ranking trait
 *
 * @param rankingsMap - a HashMap[Int, Double] representing each Int id in
 * the corpus and its corresponding page ranking
 */
class PageRanking(rankingsMap: HashMap[Int, Double]) extends Ranking[Int] {

  override def rank(id: Int) = rankingsMap(id)
}