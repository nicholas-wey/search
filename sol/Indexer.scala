package search.sol
import scala.xml.Node
import scala.xml.NodeSeq
import scala.util.matching.Regex
import search.src.StopWords
import search.src.PorterStemmer
import scala.collection.mutable._
import collection.mutable.HashSet
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.FileNotFoundException

/**
 * a class that pre-processes a Wiki corpus to create index files, which
 * store information about the relevance of terms to documents
 *
 * @param wikiFileName - a String representing the filepath to the wiki corpus
 * @param indexFileName - a String representing the filepath to the main index
 * file storing the allDocs HashMap which will be written to by the indexer
 * @param titleFileName - a String representing the filepath to the title index
 * file which will be written to by the indexer
 * @param idfFileName - a String representing the filepath to the inverse
 * document frequency index file which will be written to by the indexer
 */
class Indexer(wikiFileName: String, indexFileName: String,
              titleFileName: String, idfFileName: String) {
  val mainNode: Node = xml.XML.loadFile(wikiFileName)
  val pageSeq: NodeSeq = mainNode \ "page"
  val idSeq: NodeSeq = mainNode \\ "id"
  val titleSeq: NodeSeq = mainNode \\ "title"
  val allDocs = new HashMap[Int, Page]
  val idfMap = new HashMap[String, Double]
  val titleToIDMap = new HashMap[String, Int]
  val numPages = pageSeq.length
  val linkStructure = new HashMap[Int, HashSet[Int]]

  /**
   * a method that hashes every title and corresponding ID in the Wiki corpus
   * as a key-value pair into our titleToIDMap HashMap
   */
  private def makeTitleIdMap {
    val zip = titleSeq.zip(idSeq)
    zip.foreach(_ match {
      case (title, id) =>
        titleToIDMap.put(title.text.trim, id.text.trim.toInt)
    })
  }

  /**
   * a method that tokenizes a string representing the text of a wiki page
   * into a list of terms, which include words and numbers, but with all
   * punctuation and whitespace removed
   *
   * @param text - a String representing the text to be tokenized
   * @return - a list of Strings, with each string representing a term from
   * the original text
   */
  private def tokenize(text: String): List[String] = {
    val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+’[^\W_]+|[^\W_]+""")
    val matchesIterator = regex.findAllMatchIn(text)
    val matchesList = matchesIterator.toList.map { aMatch => aMatch.matched }
    matchesList
  }

  /**
   * a method that filters out all of the stop words from a list of terms
   *
   * @param terms - a list of Strings, with each string representing a term
   * from the original text
   * @return - the inputed list, with all stop words (the most common words
   * in the language) removed
   */
  private def stopWords(terms: List[String]): List[String] = {
    terms.filter(x => !(StopWords.isStopWord(x)))
  }

  /**
   * a method that stems each word from a list of terms
   *
   * @param terms - a list of Strings, with each string representing a term
   * from the original text
   * @return - the inputed list, with each stemmed (i.e. lowercase, and without
   * any part of the word that is not at its root)
   */
  private def stemming(terms: List[String]): List[String] = {
    val lowerCaseTxt = terms.map(x => x.toLowerCase)
    val stemmed = lowerCaseTxt.map(x => PorterStemmer.stem(x))
    stemmed
  }

  /**
   * a method that creates a Page object for each page in the Wiki corpus,
   * which includes that page's title, PageRank, and term-frequency HashMap,
   * and hashes that Page's id and corresponding Page object as a key-value pair
   * into the allDocs HashMap. allDocs represents every document in our corpus.
   */
  private def makeAllDocs {
    pageSeq.foreach(hashPageObject)
  }

  /**
   * a helper method for linkHelper, which takes an Option[Int] and an idInt,
   * and hashes a key-value pair of (idInt, Int) into our linkStructure HashMap
   * if the Option contains something (i.e. the link in the document actually
   * links to another document in the corpus)
   *
   * @param x - an Option[Int], Some(n) if the link that this Option refers
   * to actually refers to another ID n in the wiki corpus, and None if it doesn't
   * @param idInt - an Int representing the id of the document which contains
   * this original link in its Text
   */
  private def helper2(x: Option[Int], idInt: Int) {
    x match {
      case None => return
      case Some(id) =>
        if (linkStructure.contains(idInt)) {
          val Some(idSet) = linkStructure.get(idInt)
          val newSet = idSet.+(id)
          linkStructure.put(idInt, newSet)
        } else {
          val emptySet = new HashSet[Int]()
          val newSet = emptySet.+(id)
          linkStructure.put(idInt, newSet)
        }
    }
  }

  /**
   * a method that takes in a tokenized link, parses the text out of it and
   * adds it back into the termsList to be processed by the indexer, and
   * finally adds that link into the linkStructure HashMap if that link
   * actually refers to another document in the Wiki Corpus
   *
   * @param link - a String representing the link
   * @param idInt - an int representing the id of the document which contains
   * this link in its text
   */
  private def linkHelper(link: String, idInt: Int): List[String] = {
    val tlink = link.substring(2, link.length - 2)
    if (tlink.contains('|')) {
      val splitArr = tlink.split('|')
      val idOption = titleToIDMap.get(splitArr(0))
      helper2(idOption, idInt)
      if (splitArr.length == 1) {
        List[String]()
      } else {
        tokenize(splitArr(1))
      }
    } else {
      val idOption = titleToIDMap.get(tlink)
      helper2(idOption, idInt)
      tokenize(tlink)
    }
  }

  /**
   * a method that does most of the preprocessing work in our indexer. it takes
   * a page, parses / tokenizes / filters for stop words / stems its text,
   * scores the relevance of each term in a HashMap created for that document,
   * creates a new Page object, which includes that page's title, PageRank, and
   * term-frequency HashMap, and hashes that Page's id and corresponding Page
   * object as a key-value pair into the allDocs HashMap. it also adds to the
   * inverse document frequency HashMap after creating the final term frequency
   * map for each page.
   *
   * @param doc - a Node representing a page of the wiki corpus
   */
  private def hashPageObject(doc: Node) {
    val termFreqMap = new HashMap[String, Double]
    val title = doc \ "title"
    val text = doc \ "text"
    val id = doc \ "id"
    val idInt = id.text.trim.toInt
    var max = 0.0
    var termsList = tokenize(text.text.trim + " " + title.text.trim)
    for (str <- termsList) {
      if (str.matches("""\[\[[^\[]+?\]\]""")) {
        termsList = termsList ::: linkHelper(str, idInt)
      }
    }
    termsList = stopWords(termsList)
    termsList = stemming(termsList)
    for (str <- termsList) {
      if (str.matches("""\[\[[^\[]+?\]\]""")) {
        null
      } else if (termFreqMap.contains(str)) {
        val Some(double) = termFreqMap.get(str)
        val newDouble = double + 1.0
        termFreqMap.put(str, newDouble)
        if (max < newDouble) {
          max = newDouble
        }
      } else
        termFreqMap.put(str, 1.0)
      if (max < 1.0) {
        max = 1.0
      }
    }
    for ((k, v) <- termFreqMap) {
      termFreqMap.put(k, v / max)
      if (idfMap.contains(k)) {
        val Some(double) = idfMap.get(k)
        val newDouble = double + 1.0
        idfMap.put(k, newDouble)
      } else
        idfMap.put(k, 1.0)
    }
    val page = new Page(title.text.trim, -1.0, termFreqMap)
    allDocs.put(idInt, page)
  }

  /**
   * a method that iterates through each key-value pair in the inverse document
   * frequency HashMap and for each value, applies the formula specified in the
   * search pdf documentation to calculate the inverse document frequency of
   * that term
   */
  private def idfFinalCalc {
    for ((k, v) <- idfMap) {
      idfMap.put(k, Math.log(numPages / v))
    }
  }

  /**
   * a method that calculates each PageRank score for each page in the Wiki
   * corpus based on the linking structure between each page. then, each
   * PageRank field in each Page object for every page  is set to its correct
   * PageRank score
   */
  private def makePageRanks {
    val linkStruct = new MapLinkStructure(linkStructure)
    val pRanks = new PageRank(numPages, idSeq)
    val ranking = pRanks.computePageRank(linkStruct)
    for ((k, v) <- allDocs) {
      val pr = ranking.rank(k)
      v.setPageRank(pr)
    }
  }

  /**
   * a method that writes each key-value pair in the titleToIDMap into a
   * designated titles index file which stores a textual representation of
   * each id and its corresponding title
   */
  private def makeTitlesFile {
    val bw = new BufferedWriter(new FileWriter(titleFileName))
    try {
      for ((k, v) <- titleToIDMap) {
        val line = v.toString + "  " + k
        bw.write(line)
        bw.newLine
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        bw.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }
  }

  /**
   * a method that writes each key-value pair in the inverse document frequency
   * map into a designated idf index file which stores a textual representation of
   * each term and its corresponding inverse document frequency
   */
  private def makeidfMapFile {
    val bw = new BufferedWriter(new FileWriter(idfFileName))
    try {
      for ((k, v) <- idfMap) {
        val line = k + " " + v.toString
        bw.write(line)
        bw.newLine
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        bw.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }
  }

  /**
   * a method that writes each key-value pair in the allDocs into a
   * designated index file, which stores a textual representation of
   * each id and its corresponding page object
   */
  private def makeIndexFile {
    val bw = new BufferedWriter(new FileWriter(indexFileName))
    try {
      for ((k, v) <- allDocs) {
        val line = "ID " + k.toString
        val line2 = "TITLE " + v.title
        val line3 = "PR " + v.pageRank
        bw.write(line)
        bw.newLine
        bw.write(line2)
        bw.newLine
        bw.write(line3)
        bw.newLine
        for ((str, double) <- v.tfMap) {
          val line4 = str + " " + double.toString
          bw.write(line4)
          bw.newLine
        }
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        bw.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }

  }
}

/**
 * a companion object for the indexer class, contains a main method which
 * executes the pre-processing and index file-writing steps for a given
 * wikipedia corpus
 */
object Indexer {
  def main(args: Array[String]) {
    if (args.length != 4) {
      println("Please give valid arguments to the Indexer.")
    } else {
      try {
        val x = new Indexer(args(0), args(1), args(2), args(3))
        println("Indexer is indexing wiki corpus...")
        x.makeTitleIdMap
        x.makeAllDocs
        x.idfFinalCalc
        x.makePageRanks
        x.makeTitlesFile
        x.makeIndexFile
        x.makeidfMapFile
        println("Indexing is complete!")
      } catch {
        case e: FileNotFoundException =>
          println("Please enter valid filepaths.")
      }
    }
  }
}