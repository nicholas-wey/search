package search.src

/**
 * A very inconvenient implementation of Ranking. The IDs of the
 * documents are assumed to be integers from 0 to the length of the
 * given array
 *
 * @param ranking An array of scores/rankings
 */
class ArrayRanking(ranking: Array[Double]) extends Ranking[Int] {

  override def rank(id: Int) = ranking(id)

}
