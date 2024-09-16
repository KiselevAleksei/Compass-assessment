Take a list of contact information (from an Excel file) and identify which contacts are potentially duplicates.

1.	Identify which contacts are possible matches—using the string similarity algorithm library (Jaro-Winkler).
    It can be used to measure how similar two strings are, even if they don't match exactly. This also allows us
  	to estimate the score for each match of the elements.
2.  A contact might have multiple or no matches
4.	All processing is done in working memory (no database).
5.	For better performance use HashMap indicies and multithreading procession with ExecutorService.
