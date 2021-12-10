# search
Search engine for a corpus of Wikipedia pages

INSTRUCTIONS FOR USE:

VERY IMPORTANT: When running on the terminal, the JVM will hit an OutOfMemory error when running the BigWiki. In order to minimize runtime, with files this large, please run WITH the -J-Xmx2g command (giving the JVM 2 GB memory). The -J-Xmx2g command can be used for smaller files as well, but is absolutely necessary for BigWiki. THIS MUST BE DONE FOR THE INDEXER AND THE QUERIER.

In order to make a free text query into a wiki corpus, the user first needs to run the Indexer in order to pre-process the corpus into index files for the querier to access. On the command line, the user must run Indexer.scala, with four arguments (in the following order):

1) a file path to the wiki corpus

2) a file path for a main indexer .txt file, which stores the relevant information about each document in the corpus

3) a file path for a titles .txt file, which stores the relevant information matching id’s to corresponding titles in the corpus

4) a file path for the inverse document frequency .txt file, which stores the relevant information matching terms in the corpus to their idf value

For example, one could navigate to the “course/cs0180/workspace/scalaproject/bin” directory and run the following for the BigWiki corpus:

“scala -J-Xmx2g search.sol.Indexer /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/BigWiki /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/index.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/titles.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/idfMap.txt”

This runs the Indexer, a program that pre-processes a Wiki corpus to create index files, which store information about the relevance of terms to documents.

Now, the user is ready to make a free text query in the Querier program. In order to make this query, the user must run Querier.scala on the command line with the following arguments (in the following order):

1) an OPTIONAL argument: “--pagerank”, which factors-in each page’s page rank in the corpus when calculating document relevance, or “--smart”. “--smart” does not actually effect the normal query, as we did not choose to implement this special feature. Still, we can handle this input.

2) a file path to the indexer .txt file, which stores the relevant information about each document in the corpus (logically, this should be the corresponding .txt file that the indexer just wrote to) 

3) a file path to the titles .txt file, which stores the relevant information matching id’s to corresponding titles in the corpus (logically, this should be the corresponding .txt file that the indexer just wrote to) 

4) a file path to the inverse document frequency .txt file, which stores the relevant information matching terms in the corpus to their idf value (logically, this should be the corresponding .txt file that the indexer just wrote to) 

For example, one could navigate to the “course/cs0180/workspace/scalaproject/bin” directory and run the following if they wanted to make a “normal” query for the BigWiki corpus: 

“scala -J-Xmx2g search.sol.Querier /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/index.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/titles.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/idfMap.txt”


Or, one could navigate to the “course/cs0180/workspace/scalaproject/bin” directory and run the following if they wanted to make a “pageranked” query for the BigWiki corpus: 

“scala -J-Xmx2g search.sol.Querier --pagerank /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/index.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/titles.txt /gpfs/main/home/nwey/course/cs0180/workspace/scalaproject/sol/search/sol/idfMap.txt”

Now, the Querier will quickly load the information provided in the index files, and enter a REPL loop, prompting the use to make a query. The user can now type a free text query into the command line. The Querier will process the query by finding and returning a list of the titles of the top ten most relevant documents to that query.

NOTE: If the terms in the query do not match any documents or includes only stop words, the querier will return this message: "No pages contain this query or it is a stop word.”

The user can continue entering different queries into that corpus via the command line, but can exit the Querier at any time by typing “:quit” or ‘Ctrl-D’.

In order to query on a new different corpus, the user must first re-index that new corpus by running the same Indexer command with the wiki corpus file path changed, and then can re-run the querier on the newly indexed files.
_________________________________________________________________________________________________

DESIGN OVERVIEW:   

INDEXER:

General Notes: 

The general design of our Indexer pre-processes our Wiki corpus into the textual representations of three different data structures repressing the corpus. One is an inverse document frequency HashMap, which contains all the terms in the corpus and their IDF value. One is a id-title HashMap, which contains all the id’s in the corpus and their corresponding titles. The last is an HashMap which contains a key-value pair for every page in the wiki corpus, storing an id and its corresponding Page object. That is, we created a Page class to represent a page, which stores various data for that page its fields: title, PageRank, and a term-frequency HashMap. From these three HashMap data structures, we are able to implement all the functionalities we need in our Querier quickly and accurately. The following explains how we build each of these data structures, and then convert them into .txt files:

Our indexer works by instantiating an indexer object, and setting values that will be used later by parsing the wiki corpus. These include various NodeSeq fields which represent sequences of Page, ID, and Title Nodes, as well as numPages, for example, which is simply the number of pages in the wiki corpus. Then the program calls various methods on this instance of Indexer that pre-process the wiki corpus. They are (in the following order):

1) makeTitleIdMap - a method that hashes every title and corresponding ID in the Wiki corpus as a key-value pair into our titleToIDMap HashMap

2) makeAllDocs - a method that calls HashPageObject for each page in the corpus. HashPageObject is a method that creates a Page object for each page in the Wiki corpus, which includes that page's title, PageRank, and term-frequency HashMap, and hashes that Page's id and corresponding Page object as a key-value pair into the allDocs HashMap. The term-frequency HashMap is found by tokenizing, removing stop words, and stemming the page’s text into a list. Then it updates the term-frequency HashMap for each term in that list. All the while, links in that document are added to the LinkStructure HashMap if they refer to another page in the Corpus (which is why we have titleToIDMap). And finally, once the list is run through, the inverse document frequency HashMap is updated. Again, makeAllDocs repeats this method for each page, and so our allDocs HashMap represents a HashMap storing fields for every document in our corpus.

3) idfFinalCalc - a method that iterates through each key-value pair in the inverse document frequency HashMap and for each value, applies the formula specified in the search pdf documentation to calculate the actual inverse document frequency of that term.

4) makePageRanks -  a method that calculates each PageRank score for each page in the Wiki corpus based on the linking structure between each page. This method also sets each PageRank field in each Page object to its correct PageRank score. The specifics of this are discussed later in the PAGERANK part of the DESIGN OVERVIEW section of the README.

5) makeTitlesFile, makeidfMapFile, makeIndexFile - methods that write all the relevant information from the wiki corpus into index files to store this info (i.e. id-title pairs, term-inverse document frequency pairs, and id-page object pairs.)

QUERIER:

General Notes:

The general design of our Querier first instantiates an instance of Querier, which in its constructor takes in the textual representations of the three different data structures representing the corpus (an inverse document frequency HashMap, an id-title HashMap, and the id-page object HashMap). It also takes in an optionalArg Int, representing if --smart, —page rank, or no optional arguments were entered. The instance of Querier then reconstructs these data structures as fields in its body, and finally runs a REPL instructing the user to enter a free text query. The following explains how this works:

Our querier works by calling various methods that load the index files and run the query REPL. They are (in the following order):

1) makeIDFMap, makeIdTitlesMap, makeQuerierMap - methods that construct the three HashMap data structures for storing the respective key-value pairs that correspond to the idfMap, idTitlesMap, and querierMap. They do so by reading through each line, and executing commands based on what those lines return, as well taking advantage of our own knowledge of how we constructed each file respectively. For example, for the inverse document frequency HashMap, we know that each line of its corresponding index file will contain one key-value pair on each line, separated by a space. So we can easily just split each line by the space, add each key-value pair as (array(0), array(1)) to our idf HashMap, and move on to the next line. It is a little more complex with making the id-page object HashMap, as we must create a term frequency HashMap for every page along the way, but the general reasoning is still the same.

2) runQuerier - a method that actually runs the query: prompting the user for input by calling the getQuery helper; tokenizing, removing stop words, stemming the query; calculating the TF*IDF score of each page, which requires iterating through each page in the id-page HashMap, and calculating the sum of the TF*IDF scores for each term in the query for that Page (calculations specified by the PDF); multiplying that score by that Page’s page rank (if that functionality was specified by the user); adding that score to a 10-element array representing the top ten documents returned by the query, if that document’s score is higher than the score of the lowest-ranked document in the top ten documents array; finally, once all pages are iterated through, the topTenDocs array is sorted based on score and printed with the printQuery method. Since this is a REPL, the user is prompted again for queries unless “:quit” or ‘Ctrl-D’ is entered, which sets a boolean that indicates an exiting of the while loop which runQuerier continuously executes to make the REPL.

NOTE: We looked at code from Show.IO from the Showdown src to help us write the getQuery user input method

PAGERANK:

As the PDF specifies, we implement PageRank through a PageRank class which extends Ranker[Int] and therefore contains the computePageRank method, which takes in a LinkStructure and outputs an object which extends Ranking[Int]. Our linkStructure is a HashMap[Int, Set[Int]] which contains all the IDs of documents that link to other documents in the corpus as keys, and the Set of ID’s that those keys link to as the value. This linkStructure gets passed into the constructor of our MapLinkStructure. The instantiated MapLinkStructure then gets inputed into the computePageRank method after the user instantiates a PageRank. Finally, our Ranker object that we output is a PageRanking class, which takes in the final HashMap[Int, Double] which represents a document’s id and its corresponding double PageRank.

In terms of actual implementation, we follow the PageRank algorithm provided by the PDF in order to compute PageRank in our computePageRank method. We pass in numPages into our PageRank constructor and idSeq, as our LinkStruture only contains id’s of pages that link to other pages. In order to get ALL the id’s in the corpus, we then have to pass in these values.

We make an instance of the PageRank class in our Indexer, and after instantiating and then passing the required fields into it (as specified int the first paragraph), we are able to compute the page rank for every document in the corpus.

EFFICIENCY: 

We believe that our Search program is efficient. The Indexer pre-processes the BigWiki in about 4-5 minutes. The Querier takes about 25 seconds to load the Big Wiki index files, and returns results almost instantaneously when in the query REPL. As would be expected, the runtime of Indexer and Querier are dependent on the size of the Wiki corpus. Pre-processing MedWiki took about 30 seconds, and pre-processing Small Wiki was usually under 10 seconds.
