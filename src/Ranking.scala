package search.src

/**
 * A trait that describes the ranking of various IDs in a corpus
 */
trait Ranking[T] {

  /**
   * Returns the ranking of a particular ID
   *
   * @param An ID
   * @return The ranking of the given ID
   */
  def rank(id: T): Double

}
