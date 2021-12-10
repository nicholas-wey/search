package search.sol
import collection.mutable.HashMap
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader;
import scala.util.matching.Regex
import search.src.StopWords
import search.src.PorterStemmer
import scala.util.Sorting.stableSort
import java.io.FileNotFoundException

/**
 * a class that runs queries, taking as input the index files produced by
 * our index program, as well as optional arguments. it prompts a user to
 * enter a free text query and then answers it with most relevant pages
 * titles.
 *
 * @param optionalArg - an Int, 1, 2, or 0 representing if the user inputed
 * --pagerank, --smart, or no optional arguments
 * @param mainIndexFileName - a String representing the file path to the index
 * file storing information to help create the querierMap HashMap (the main
 * one with all the documents and their corresponding term-frequency maps)
 * @param titlesFileName - a String representing the file path to the index
 * file storing information to help create the idTitles HashMap
 * @param idfIndexFileName - a String representing the file path to the index
 * file housing information to help create the idfMap HashMap
 */
class Querier(optionalArg: Int, mainIndexFileName: String,
              titlesFileName: String, idfFileName: String) {

  val idTitlesMap = new HashMap[Int, String]
  val querierMap = new HashMap[Int, Page]
  val idfMap = new HashMap[String, Double]
  var end = false
  var eofReached = false
  var termInCorpus = false
  var containsWords = false

  /**
   * a method that constructs the inverse document frequency HashMap
   */
  private def makeIDFMap {
    val br = new BufferedReader(new FileReader(idfFileName))
    try {
      var line = br.readLine
      while (line != null) {
        val arr = line.split(" ")
        idfMap.put(arr(0), arr(1).toDouble)
        line = br.readLine
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        br.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }
  }

  /**
   * a method that constructs the idTitlesMap HashMap
   */
  private def makeIdTitlesMap {
    val br = new BufferedReader(new FileReader(titlesFileName))
    try {
      var line = br.readLine
      while (line != null) {
        val arr = line.split("  ")
        idTitlesMap.put(arr(0).toInt, arr(1))
        line = br.readLine
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        br.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }
  }

  /**
   * a method that constructs that querierMap
   */
  private def makeQuerierMap {
    val br = new BufferedReader(new FileReader(mainIndexFileName))
    try {
      var line = br.readLine
      while (line != null) {
        val id = line.substring(3).toInt
        var line2 = br.readLine
        val title = line2.substring(6)
        var line3 = br.readLine
        val pageRank = line3.substring(3).toDouble
        var line4 = br.readLine
        val tfMap = new HashMap[String, Double]
        while (line4 != null && line4.substring(0, 2) != "ID") {
          val arr = line4.split(" ")
          tfMap.put(arr(0), arr(1).toDouble)
          line4 = br.readLine
        }
        val page = new Page(title, pageRank, tfMap)
        querierMap.put(id, page)
        line = line4
      }
    } catch {
      case e: IOException => "Encountered an error: " + e.getMessage
    } finally {
      try {
        br.close
      } catch {
        case e: IOException =>
          "Encountered an error closing the writer: " + e.getMessage
      }
    }
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
    val regex = new Regex("""[^\W_]+’[^\W_]+|[^\W_]+""")
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
  private def stopWords(text: List[String]): List[String] = {
    text.filter(x => !(StopWords.isStopWord(x)))
  }

  /**
   * a method that stems each word from a list of terms
   *
   * @param terms - a list of Strings, with each string representing a term
   * from the original text
   * @return - the inputed list, with each stemmed (i.e. lowercase, and without
   * any part of the word that is not at its root)
   */
  private def stemming(text: List[String]): List[String] = {
    val lowerCaseTxt = text.map(x => x.toLowerCase)
    val stemmed = lowerCaseTxt.map(x => PorterStemmer.stem(x))
    stemmed
  }

  /**
   * a method that prompts the user to enter a query, and gets the input from
   * the command line, returning it as a string, or signaling to end the query
   * if ":quit" or 'Ctrl-D' is entered
   *
   * @return - a String representing the user input
   */
  private def getQuery: String = {
    println("Please make a query into the wiki corpus.")
    val br = new BufferedReader(new InputStreamReader(System.in))
    try {
      val query = br.readLine
      eofReached = query == null
      query
    } catch {
      case e: IOException =>
        println("Error reading user input, exiting.")
        System.exit(1)
        null
    }
  }

  /**
   * a method that prints the relevant pages for a free text query
   *
   * @param topTenArr - an IdScorePair Array representing up to the 10 most
   * relevant pages to the free text query
   */
  private def printQuery(topTenArr: Array[IdScorePair]) {
    if (termInCorpus == false) {
      println("No pages contain this query or it is a stop word.")
    } else {
      for (i <- 0 to topTenArr.length - 1) {
        val pair = topTenArr(i)
        if (pair.finalScore >= 0.0) {
          val Some(title) = idTitlesMap.get(pair.id)
          println(i + 1 + ". " + title)
        }
      }
    }
    termInCorpus = false
  }

  /**
   * a method that runs the query to calculate which pages are most relevant
   * to the query
   */
  private def runQuerier {
    while (end == false) {
      val topTenDocs = new Array[IdScorePair](10)
      for (i <- 0 to topTenDocs.length - 1) {
        val dummy = new IdScorePair(-1, -1.0)
        topTenDocs(i) = dummy
      }
      var minDocScore = -1.0
      var minDocIndex = 0
      var line = this.getQuery
      if (line == ":quit" || eofReached == true) {
        end = true
        println("Exiting the Query.")
        return
      }
      val processedQuery = stemming(stopWords(tokenize(line)))
      for ((id, page) <- querierMap) {
        var pageScore = 0.0
        containsWords = false
        for (term <- processedQuery) {
          if (page.tfMap.contains(term)) {
            containsWords = true
            val Some(tf) = page.tfMap.get(term)
            val Some(idf) = idfMap.get(term)
            val x = tf * idf
            pageScore += x
            termInCorpus = true
          }
        }
        if (optionalArg == 1) {
          pageScore = pageScore * page.pageRank
        }
        if (pageScore > minDocScore && containsWords == true) {
          val newDoc = new IdScorePair(id, pageScore)
          topTenDocs(minDocIndex) = newDoc
          var minS = topTenDocs(0).finalScore
          var minI = 0
          for (i <- 1 to topTenDocs.length - 1) {
            if (topTenDocs(i).finalScore < minS) {
              minS = topTenDocs(i).finalScore
              minI = i
            }
          }
          minDocScore = minS
          minDocIndex = minI
        }
      }
      stableSort(topTenDocs, (x: IdScorePair, y: IdScorePair) =>
        (x.finalScore > y.finalScore))
      printQuery(topTenDocs)
    }
  }
}

/**
 * a companion object for Querier, contains a main method which prompts a user
 * to input a query, and runs the query by calculating and returning the most
 * relevant pages to the query as a list of titles
 */
object Querier {
  def main(args: Array[String]) {
    if (args.length > 4 || args.length < 3) {
      println("Please give valid arguments to the Querier")
    } else {
      try {
        if (args(0) == "--pagerank") {
          val querier = new Querier(1, args(1), args(2), args(3))
          println("Please wait while the querier loads...")
          querier.makeIDFMap
          querier.makeIdTitlesMap
          querier.makeQuerierMap
          querier.runQuerier
        } else if (args(0) == "--smart") {
          val querier = new Querier(2, args(1), args(2), args(3))
          println("Please wait while the querier loads...")
          querier.makeIDFMap
          querier.makeIdTitlesMap
          querier.makeQuerierMap
          querier.runQuerier
        } else {
          val querier = new Querier(0, args(0), args(1), args(2))
          println("Please wait while the querier loads...")
          querier.makeIDFMap
          querier.makeIdTitlesMap
          querier.makeQuerierMap
          querier.runQuerier
        }
      } catch {
        case e: FileNotFoundException =>
          println("Please enter valid filepaths.")
      }
    }
  }
}