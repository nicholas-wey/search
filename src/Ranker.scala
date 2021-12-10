package search.src

/**
 * A trait that computes the page rank for some LinkStructure
 */
trait Ranker[T] {

  final val Epsilon = 0.001
  final val DampeningFactor = 0.15

  /**
   * Computes the page rank of the IDs in the given Link Structure
   *
   * @param linkStructure The structure of the links
   * @return The ranking of the various IDs
   */
  def computePageRank(linkStructure: LinkStructure[T]): Ranking[T]

}
