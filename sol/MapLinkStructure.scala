package search.sol

import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import search.src.LinkStructure

/**
 * a class which extends LinkStructure[T], and implements methods for accessing
 * different attributes of our link structure
 *
 * @param linksHashSet - an HashMap[Int, HashSet[Int]] which stores an id and
 * every id that that id's document links to
 */
class MapLinkStructure(linksHashMap: HashMap[Int, HashSet[Int]]) extends LinkStructure[Int] {

  override def ids: Iterable[Int] = {
    var idList = List[Int]()
    for ((k, v) <- linksHashMap) {
      idList :+ k
    }
    idList
  }

  override def links(id: Int): HashSet[Int] = {
    val emptySet = new HashSet[Int]()
    val x = linksHashMap.get(id)
    x match {
      case None      => emptySet
      case Some(set) => set
    }
  }
}