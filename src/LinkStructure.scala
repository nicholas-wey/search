package search.src

import scala.collection.mutable.Set

/**
  * A trait that describes the link structure of a corpus
  */
trait LinkStructure[T] {

  /**
    * The IDs in the corpus
    *
    * @return The IDs
    */
  def ids: Iterable[T]

  /**
    * The set of IDs that a particular ID links to
    *
    * @param id The ID
    * @return The IDs that id links to
    */
  def links(id: T): Set[T]
}
