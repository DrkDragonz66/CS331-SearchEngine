import java.nio.file.*;
import java.io.*;
import java.util.*; //regex, Array, ArrayList, List, Iterator, Map, among others

public class Parser {
public static void main(String[] args) throws IOException {
	String corpusDir = "", invertedIndexFile = "", stopListFile = "", queriesFile = "", resultsFile = "";
	
	for (int i = 0; i < args.length; i++) { //go through arguments
		if (args[i].startsWith("-")) { //is flag is there
			if (i != args.length && !args[i+1].isEmpty()) { //if not the end of the list and next index is not empty
				
				if(args[i].equals("-CorpusDir")){
					if (!corpusDir.isEmpty()) {
						System.out.println("2 instances of CorpusDir flag, exiting program");
						System.exit(1);
					}
					else corpusDir = args[i+1];
				}//end CorpusDir
				
				if(args[i].equals("-InvertedIndex")){
					if (!invertedIndexFile.isEmpty()) {
						System.out.println("2 instances of invertedIndex flag, exiting program");
						System.exit(1);
					}
					else invertedIndexFile = args[i+1];
				}//end InvertedIndex
				
				if(args[i].equals("-StopList")){
					if (!stopListFile.isEmpty()) {
						System.out.println("2 instances of StopList flag, exiting program");
						System.exit(1);
					}
					else stopListFile = args[i+1];
				}//end StopList
				
				if(args[i].equals("-Queries")){
					if (!queriesFile.isEmpty()) {
						System.out.println("2 instances of Queries flag, exiting program");
						System.exit(1);
					}
					else queriesFile = args[i+1];
				}//end Queries
				
				if(args[i].equals("-Results")){
					if (!resultsFile.isEmpty()) {
						System.out.println("2 instances of Results flag, exiting program");
						System.exit(1);
					}
					else resultsFile = args[i+1];
				}//end Results
			}//end setting variables
		} //end flag
	}// end args
	
	if (corpusDir.isEmpty()) {
		System.out.println("CorpusDir was not provided but is required, exiting program");
		System.exit(1);
	}
	if (invertedIndexFile.isEmpty()) {
		System.out.println("invertedIndex was not provided, defaulting to InvertedIndex.txt");
		invertedIndexFile = "InvertedIndex.txt";
	}
	if (stopListFile.isEmpty()) {
		System.out.println("StopList was not provided, parsing will not have a StopList.");
	}
	
	if (queriesFile.isEmpty()) {
		System.out.println("Queries was not provided but is required, exiting program");
		queriesFile = "InvertedIndex.txt";
		System.exit(1);
	}
	else {
		if (resultsFile.isEmpty()) {
			System.out.println("Results was not provided, defaulting to Results.txt");
			resultsFile = "Results.txt";
		}
	}	
	
	Map<String, List<IndexItem>> InvertedIndex = new TreeMap<String, List<IndexItem>>(); //treemap for alphabetical order
	//Map has to be laid out this way, as the only key that is valid are the filenames
	//because if words were to be the key, multiple of the words in different files will override each other.
	//The value of the map is also a List of Object IndexItem which holds the word and its count.
	//So it acts like <filename, word, count>
	
	//String currentDirectory = System.getProperty("user.dir");
	//currentDirectory += "\\Corpus"; //go to Corpus folder
	//File folder = new File(currentDirectory); //should return corpus folder
	
		File folder = new File(corpusDir); //should return corpus folder
		//System.out.println(currentDirectory); //DEBUG check directory
		File[] listOfFiles = folder.listFiles(); //array of files in the folder
		
		List<String[]> stopWords;
		if (stopListFile.isEmpty()) stopWords = new ArrayList<String[]>(); //no stopList is given so default to empty
		else stopWords = readTextFileByLines(corpusDir + "\\" + stopListFile); //read stopListFile
				
		int filecounter = 0;
		for (File file : listOfFiles) {
			if (getFileExtension(file).equals("html")) { //only want to parse html files
				String filename = file.getName(); //get filename
				String content = parseTextFile(corpusDir + "\\" + filename); //get content of html file
						        
		        List<String> contentWords = splitContent(content, stopWords);
		        InvertedIndex = indexWebsite(InvertedIndex, contentWords, filename);
		    } //end parse HTML file
			filecounter++;
	        if(filecounter % 10 == 0) System.out.print("| Parsing " + filecounter + "\\" + listOfFiles.length + " | ");
	        if(filecounter%30 == 0) System.out.println();
	        //if (filecounter == 30) break;
		} //end for listOfFiles
		System.out.println();
		System.out.println("Parsing finished, " + filecounter + " HTML files have been parsed");
		
		printInvertedIndex(InvertedIndex, false, corpusDir + "\\" + invertedIndexFile);
		System.out.println("Saved InvertedIndex to " + corpusDir + "\\" + invertedIndexFile);
		
		List<String[]> queryList = readTextFileByLines(corpusDir + "\\" + queriesFile); //read queriesFile
		List<String> results = searchIndex(InvertedIndex, queryList);
		
		
		
		printResults(results, false, corpusDir + "\\" + resultsFile);
		System.out.println("Saved Results to " + corpusDir + "\\" + resultsFile);
	} //end main

public static String getFileExtension(File file) {
    String fileName = file.getName(); //get filename
    if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
    	//if there is a . signaling a file extension at the end
    return fileName.substring(fileName.lastIndexOf(".")+1); 
    	//returns everything after the . such as 'html'
    else return ""; //else return blank
}


public static String parseTextFile(String fileName) throws IOException {
    String content = new String(Files.readAllBytes(Paths.get(fileName)));
    //converts the file into a string
    return content;
}


public static List<String> splitContent(String content, List<String[]> sW) throws IOException {
	String c[] = content.split(" "); //split the content into multiple strings based on spaces
    List<String> words = new ArrayList<String>(); //create an ArrayList of strings to hold the multiple strings
    		words.addAll(Arrays.asList(c)); //add the multiple strings to the ArrayList 
    		
    		List<String> stopWords = new ArrayList<String>();
    		for (String[] s: sW) {
    			stopWords.add(s[0]); //add stopWords to its own List because the first list is a String[] because of queries format
    		}

    Iterator<String> iterator = words.iterator(); //Create an iterator for the ArrayList
    //Iterator is used so removal of the non-words does not break the iteration
    
    
    while (iterator.hasNext()) {    	
    	String w = iterator.next(); //iterates through the list of content words
    	if (w.startsWith("\"")) w = w.substring(1); //remove starting quotation marks
    	//Note that the quotes do not get removed from final list
    	
    	//Does not work for some reason?
    	if (w.matches("[a-zA-Z](.*)")) { //checks if the first character is not a letter
    		iterator.remove();
    		continue;
				}
    	if (w.matches(".*\\W")) //removes ending punctuation marks and other non-word characters
   		 w = w.substring(0, w.length()-1);
    	
    	if(stopWords.contains(w.toLowerCase())) {
    		iterator.remove();
    	}
    	
		}//end while     	   
    return words;
}


public static Map<String, List<IndexItem>> indexWebsite(Map<String, List<IndexItem>> InvertedIndex, List<String> contentWords, String filename) {
	InvertedIndex.put(filename, new ArrayList<IndexItem>()); //create index for the filename
	String word;
	int count;
	Map<String, Integer> wordCount = new TreeMap<>(); //create map for just this file, treemap for alphabetical order 
	
	for(int i = 0; i < contentWords.size(); i++) {
		word = contentWords.get(i); //get the word
		
		if (wordCount.containsKey(word)){ //if the word exists in the map
			count = wordCount.get(word); //get the word's count
			count++; //increment
			wordCount.replace(word, count); //replace with new count
		}
		else {
			if (word.startsWith("\"")) 
				word = word.substring(1); //remove starting quotation marks
						
			//possibly change this to while to remove multiple at end?
	    	if (word.matches(".*\\W")) //removes ending punctuation marks and other non-word characters
	    		 word = word.substring(0, word.length()-1);
	    		  		
			wordCount.put(word, 1); //add the word to the map, count is 1 because first
		}
	}// end first for loop
	
	for (String key: wordCount.keySet())
	{
		InvertedIndex.get(filename).add(new IndexItem(key, wordCount.get(key)));
		//adds everything in wordCount map to the InvertedIndex map
		//following the filename, word, count format
	}
	return InvertedIndex;
}
public static void  printInvertedIndex(Map<String, List<IndexItem>> InvertedIndex, boolean print, String filename) throws IOException {
	File fout = new File(filename);
	FileOutputStream fos = new FileOutputStream(fout);
 
	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
 
	for (String fname: InvertedIndex.keySet()) {
		int size = InvertedIndex.get(fname).size();
		String item;
		for (int i = 0; i < size; i++) {
			item = ""; //reset item
			item += fname + " "; //filename + space
			item += InvertedIndex.get(fname).get(i).getWord() + " "; //word + space
			item += InvertedIndex.get(fname).get(i).getCount(); //count
			
			bw.write(item);
			bw.newLine();
			if (print) System.out.println(item); //print
		}//end IndexItem
	}//end filename
	
	bw.close();
}

public static void  printResults(List<String> results, boolean print, String filename) throws IOException {
	File fout = new File(filename);
	FileOutputStream fos = new FileOutputStream(fout);
 
	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
	for (String line: results) {
		bw.write(line);
		bw.newLine();
		if (print) System.out.println(line); //print if boolean flag is true in code
	}
	
	bw.close();
}


public static List<String[]> readTextFileByLines(String fileName) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(fileName)); //reads all lines and turns a line into a single string
    List<String[]> words = new ArrayList<String[]>();
    for (String line: lines) {
    	String c[] = line.split(" "); //turn the single string into an array split on spaces, because of queries file format
    	words.add(c);
    }
    
    return words;
}

public static List<String> searchIndex(Map<String, List<IndexItem>>  InvertedIndex, List<String[]> queryList) {
	List<String> results = new ArrayList<String>();
	for (String[] query: queryList) {

		results.add(query[0] + " " + query[1]);
		for (String filename: InvertedIndex.keySet()) {
			int size = InvertedIndex.get(filename).size();
			
			for (int i = 0; i < size; i++) {
				if(InvertedIndex.get(filename).get(i).getWord().equalsIgnoreCase(query[1])) {
					String item = "";
					if (query[0].equalsIgnoreCase("query")) { //if query, all is wanted is the filename
						item = filename;
					}
					else { //otherwise it will be frequency, and output filename + count
						item = filename + " " + InvertedIndex.get(filename).get(i).getCount();
					}
					results.add(item);
					break; //break out because word only appears once no need to continue
				}//end if
			}//end for going through words	
		}//end for going through websites
	}//end queryList	
	return results;
}


}

















