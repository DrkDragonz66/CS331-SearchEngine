import java.nio.file.*;
import java.text.DecimalFormat;
import java.io.*;
import java.lang.Character;
import java.util.*;
import java.util.List;
import java.awt.*; //GUI stuff
import java.awt.event.*;
import javax.swing.*;

public class SearchEngine {

	// GUI stuff
	private static int queryCounter = 1;
	private static int pageCounter = 1;
	private static int pageStart;
	private static int pageEnd;
	private static int pages;
	private static int wordResultsCount;
	private static double recall;

	private static JLabel queryLabel = new JLabel("Showing results for <query>");
	private static JLabel pageLabel = new JLabel("Page 1 of 123456");
	private static JLabel recallLabel = new JLabel("Recall: x/y = #%");

	private static JLabel resultHeaderLabel = new JLabel("Showing results 1 - 10");
	private static JTextArea result = new JTextArea();

	private static JButton previousQuery, nextQuery, previousPage, nextPage;

	public static void main(String[] args) throws IOException {
		String corpusDir = "", PremadeInvertedIndex = "", queriesFile = "", resultsFile = "";
		String invertedIndexFileName = "", stopListFile = "", stemming = "", output = "";
		boolean porters = false;
		for (int i = 0; i < args.length; i++) { // go through arguments
			if (args[i].startsWith("-")) { // is flag is there
				if (i != args.length && !args[i + 1].isEmpty()) { // if not the end of the list and next index is not
																	// empty

					if (args[i].equals("-CorpusDir")) {
						if (!corpusDir.isEmpty()) {
							System.out.println("2 instances of CorpusDir flag, exiting program");
							System.exit(1);
						} else
							corpusDir = args[i + 1];
					} // end CorpusDir

					if (args[i].equals("-PremadeInvertedIndex")) {
						if (!PremadeInvertedIndex.isEmpty()) {
							System.out.println("2 instances of PremadeInvertedIndex flag, exiting program");
							System.exit(1);
						} else
							PremadeInvertedIndex = args[i + 1];
					} // end PremadeInvertedIndex

					if (args[i].equals("-Queries")) {
						if (!queriesFile.isEmpty()) {
							System.out.println("2 instances of Queries flag, exiting program");
							System.exit(1);
						} else
							queriesFile = args[i + 1];
					} // end Queries

					if (args[i].equals("-Results")) {
						if (!resultsFile.isEmpty()) {
							System.out.println("2 instances of Results flag, exiting program");
							System.exit(1);
						} else
							resultsFile = args[i + 1];
					} // end Results

					if (args[i].equals("-InvertedIndexFileName")) {
						if (!invertedIndexFileName.isEmpty()) {
							System.out.println("2 instances of invertedIndexFileName flag, exiting program");
							System.exit(1);
						} else
							invertedIndexFileName = args[i + 1];
					} // end InvertedIndexFileName

					if (args[i].equals("-StopList")) {
						if (!stopListFile.isEmpty()) {
							System.out.println("2 instances of StopList flag, exiting program");
							System.exit(1);
						} else
							stopListFile = args[i + 1];
					} // end StopList

					if (args[i].equals("-Stemming")) {
						if (!stemming.isEmpty()) {
							System.out.println("2 instances of stemming flag, exiting program");
							System.exit(1);
						} else
							stemming = args[i + 1];
					} // end Stemming
					
					if (args[i].equals("-Output")) {
						if (!stemming.isEmpty()) {
							System.out.println("2 instances of output flag, exiting program");
							System.exit(1);
						} else
							output = args[i + 1];
					} // end Stemming
					
				} // end setting variables
			} // end flag
		} // end args

		if (corpusDir.isEmpty()) {
			System.out.println("CorpusDir was not provided but is required, exiting program");
			System.exit(1);
		}

		if (queriesFile.isEmpty()) {
			System.out.println("Queries was not provided but is required, exiting program");
			queriesFile = "InvertedIndex.txt";
			System.exit(1);
		} else {
				if (output.equals("TEXTFILE") || output.equals("BOTH")) {
					if (resultsFile.isEmpty()) {
						System.out.println("Results was not provided, defaulting output to Results.txt");
						resultsFile = "Results.txt";
					}
				}
				else if (output.equals("GUI")) {
					System.out.println("Outputting results to the GUI");
				}
				else {
					System.out.println("Defaulting to TEXTFILE only");
					output = "TEXTFILE";
					if (resultsFile.isEmpty()) {
						System.out.println("Results was not provided, defaulting output to Results.txt");
						resultsFile = "Results.txt";
					}
			}
		} //end else

		Map<Integer, List<IndexItem>> InvertedIndex = new TreeMap<Integer, List<IndexItem>>(); // treemap for
																								// alphabetical order
		List<String> filenames = new ArrayList<String>();

		// String currentDirectory = System.getProperty("user.dir");
		// currentDirectory += "\\Corpus"; //go to Corpus folder
		// File folder = new File(currentDirectory); //should return corpus folder

		if (PremadeInvertedIndex.isEmpty()) {
			System.out.println("invertedIndexFile was not provided, building an InvertedIndex from the CorpusDir");

			if (invertedIndexFileName.isEmpty()) {
				System.out.println("invertedIndexFileName was not provided, defaulting output to InvertedIndex.txt");
				invertedIndexFileName = "InvertedIndex.txt";
			}
			if (stopListFile.isEmpty()) {
				System.out.println("StopList was not provided, parsing will not have a StopList.");
			}
			if (!stemming.equals("YES")) {
				System.out.println("Porter's Stemming Algorithm will not be implemented.");
				porters = false;
			}

			File folder = new File(corpusDir); // should return corpus folder
			// System.out.println(currentDirectory); //DEBUG check directory
			File[] listOfFiles = folder.listFiles(); // array of files in the folder

			for (File file : listOfFiles) { // goes through all the files in the CorpusDir
				if (getFileExtension(file).equals("html")) // only want to parse html files
					filenames.add(file.getName()); // add filename to its own index
			} // end for listOfFiles
			Collections.sort(filenames); // sorts by alphabet

			List<String[]> stopWords;
			if (stopListFile.isEmpty())
				stopWords = new ArrayList<String[]>(); // no stopList is given so default to empty
			else
				stopWords = readTextFileByLines(corpusDir + "\\" + stopListFile); // read stopListFile

			int filecounter = 0;

			for (String filename : filenames) { // goes through each filename, by this point it is only .html files
				filecounter++;
				List<String[]> content = readTextFileByLines(corpusDir + "\\" + filename); // read queriesFile

				InvertedIndex = indexWebsite(content, stopWords, filecounter, InvertedIndex, porters);
				// end parse HTML file

				if (filecounter % 10 == 0)
					System.out.print("| Parsing " + filecounter + "\\" + listOfFiles.length + " | ");
				if (filecounter % 30 == 0)
					System.out.println();
			} // end for filenames
			System.out.println();
			System.out.println("Parsing finished, " + filecounter + " HTML files have been parsed");

			saveInvertedIndex(InvertedIndex, filenames, false, corpusDir + "\\" + invertedIndexFileName);
			System.out.println("Saved InvertedIndex to " + corpusDir + "\\" + invertedIndexFileName);
		} // end creating InvertedIndex from CorpusDir

		else {
			filenames = getFilenamesFromPremadeInvertedIndex(corpusDir + "\\" + PremadeInvertedIndex);
			InvertedIndex = parsePremadeInvertedIndex(corpusDir + "\\" + PremadeInvertedIndex, filenames.size());
		} // end parsing provided InvertedIndex

		List<String[]> queryList = readTextFileByLines(corpusDir + "\\" + queriesFile); // read queriesFile
		Map<String, List<IndexItem>> results = searchIndex(InvertedIndex, filenames, queryList,
				corpusDir + "\\" + resultsFile, corpusDir, output);
		if (output.equals("GUI") || output.equals("BOTH"))
		resultsGUI(results, filenames);

		
	} // end main

	public static String trimSnippetHTML(String snippet) {
		snippet = snippet.trim();
		int startCounter = 0, endCounter = snippet.length() - 1;
		
		boolean found = true;
		while (found) {
			startCounter = 0;
			endCounter = snippet.length() - 1;
			if (snippet.startsWith("<")) {
				while (true) {
					if (startCounter == endCounter) {
						found = false;
						break;
					}
					startCounter++;
					if (snippet.charAt(startCounter) == '>') {
						snippet = snippet.substring(startCounter); // remove the html tag </TAG>

						startCounter = 0;
						endCounter = snippet.length() - 1;
					}
				} //end while
			} //end if
			else break;
		} // end while
		
		found = true;
		while (found) {
			endCounter = snippet.length() - 1;
			
			if (snippet.endsWith(">")) {
				while (true) {
					endCounter--;
					if (endCounter <= 0) {
						found = false;
						break;
					}
					if (snippet.charAt(endCounter) == '/') 
						if (snippet.charAt(endCounter - 1) == '<'){						
							snippet = snippet.substring(0, endCounter - 1); // remove the html tag </TAG>
							endCounter = snippet.length() - 1;
						}
				}//end while
			}//end if
			else break;
		}//end found
		
		if (snippet.length() > 100) {
			snippet = snippet.substring(0, 100);
			snippet += ". . .";
		}
		return snippet;
	}

	public static void resultsGUI(Map<String, List<IndexItem>> results, List<String> filenames) {
		pageStart = (pageCounter * 10) - 9; // 1,11,21,31,etc
		pageEnd = pageCounter * 10; // 10,20,30,40,etc

		List<String> queries = new ArrayList<String>();
		for (String query : results.keySet()) {
			queries.add(query);
		}

		// wordResultsCount = 60; //debug
		wordResultsCount = results.get(queries.get(0)).size();
		pages = (wordResultsCount / 10) + 1;
		if (pageEnd > wordResultsCount) pageEnd = wordResultsCount; //if the page ends before 10,20,30,etc
		queryLabel.setText("Showing results for " + queries.get(0)); // debug, change back to queries.get(0)
		pageLabel.setText("Showing page" + pageCounter + " of " + pages);
		resultHeaderLabel
				.setText("Showing " + pageStart + " to " + pageEnd + " out of " + wordResultsCount + " results.");
		DecimalFormat df = new DecimalFormat("###.###");
		recall = ((double)wordResultsCount / (double)filenames.size()) * 100;
		recallLabel.setText("Recall: " + wordResultsCount + "/" + filenames.size() + "=" + df.format(recall) + "%");
		JFrame frame = new JFrame("Search Engine v1");
		frame.setPreferredSize(new Dimension(1000, 1000));

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		previousQuery = new JButton("Previous Query");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 0; // top
		previousQuery.setEnabled(false); // cannot go back to anything
		previousQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (queryCounter <= 1)
					return; // if somehow button is enabled at the start of the queriesList
				queryCounter--;
				queryLabel.setText("Showing results for " + queries.get(queryCounter - 1));

				if (queryCounter == 1)
					previousQuery.setEnabled(false); // cannot go back to anything
				else
					previousQuery.setEnabled(true);
				if (queryCounter >= queries.size())
					nextQuery.setEnabled(false); // cannot go forward to anything
				else
					nextQuery.setEnabled(true);

				pageCounter = 1; // back to first page of new query
				pageStart = (pageCounter * 10) - 9; // 1,11,21,31,etc
				pageEnd = pageCounter * 10; // 10,20,30,40,etc
				wordResultsCount = results.get(queries.get(queryCounter - 1)).size();
				pages = (wordResultsCount / 10) + 1;
				if (pageEnd > wordResultsCount) pageEnd = wordResultsCount; //if the page ends before 10,20,30,etc
				pageLabel.setText("Showing page " + pageCounter + " of " + pages);
				resultHeaderLabel.setText(
						"Showing " + pageStart + " to " + pageEnd + " out of " + wordResultsCount + " results.");
				DecimalFormat df = new DecimalFormat("###.###");
				recall = ((double)wordResultsCount / (double)filenames.size()) * 100;
				recallLabel.setText("Recall: " + wordResultsCount + "/" + filenames.size() + "=" + df.format(recall) + "%");
				
				if (pageCounter == 1)
					previousPage.setEnabled(false); // cannot go back to anything
				else
					previousPage.setEnabled(true);
				if (pageCounter >= pages)
					nextPage.setEnabled(false); // cannot go forward to anything
				else
					nextPage.setEnabled(true);

				int i = 0;
				if (i == 0) { // result1
					if (pageStart + i > wordResultsCount)
						result.setText("There are no more results");
					else {
						result.setText(pageStart + i + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ " times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 0
				if (i == 1) { // result2
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 1
				if (i == 2) { // result3
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 2
				if (i == 3) { // result4
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 3
				if (i == 4) { // result5
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 4
				if (i == 5) { // result6
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get( 
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 5
				if (i == 6) { // result7
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 6
				if (i == 7) { // result8
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 7
				if (i == 8) { // result9
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 8
				if (i == 9) { // result10
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 9
			}
		}); // end addActionListener
		frame.add(previousQuery, c);

		c.gridy = 1;
		queryLabel.setSize(new Dimension(80, 40));
		frame.add(queryLabel, c);

		nextQuery = new JButton("Next Query");
		c.gridy = 2;
		nextQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (queryCounter >= queries.size())
					return; // if somehow button is enabled at the end of the queriesList
				queryCounter++;
				queryLabel.setText("Showing results for " + queries.get(queryCounter - 1));

				if (queryCounter == 1)
					previousQuery.setEnabled(false); // cannot go back to anything
				else
					previousQuery.setEnabled(true);
				if (queryCounter >= queries.size())
					nextQuery.setEnabled(false); // cannot go forward to anything
				else
					nextQuery.setEnabled(true);

				pageCounter = 1; // back to first page of new query
				pageStart = (pageCounter * 10) - 9; // 1,11,21,31,etc
				pageEnd = pageCounter * 10; // 10,20,30,40,etc
				wordResultsCount = results.get(queries.get(queryCounter - 1)).size();
				pages = (wordResultsCount / 10) + 1;
				if (pageEnd > wordResultsCount) pageEnd = wordResultsCount; //if the page ends before 10,20,30,etc
				pageLabel.setText("Showing page " + pageCounter + " of " + pages);
				resultHeaderLabel.setText(
						"Showing " + pageStart + " to " + pageEnd + " out of " + wordResultsCount + " results.");
				DecimalFormat df = new DecimalFormat("###.###");
				recall = ((double)wordResultsCount / (double)filenames.size()) * 100;
				recallLabel.setText("Recall: " + wordResultsCount + "/" + filenames.size() + "=" + df.format(recall) + "%");
				
				if (pageCounter == 1)
					previousPage.setEnabled(false); // cannot go back to anything
				else
					previousPage.setEnabled(true);
				if (pageCounter >= pages)
					nextPage.setEnabled(false); // cannot go forward to anything
				else
					nextPage.setEnabled(true);

				int i = 0;
				if (i == 0) { // result1
					if (pageStart + i > wordResultsCount)
						result.setText("There are no more results");
					else {
						result.setText(pageStart + i + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ " times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 0
				if (i == 1) { // result2
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 1
				if (i == 2) { // result3
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 2
				if (i == 3) { // result4
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 3
				if (i == 4) { // result5
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 4
				if (i == 5) { // result6
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 5
				if (i == 6) { // result7
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 6
				if (i == 7) { // result8
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 7
				if (i == 8) { // result9
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 8
				if (i == 9) { // result10
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 9
			}
		}); // end addActionListener
		frame.add(nextQuery, c);

		previousPage = new JButton("Previous Page");
		c.gridy = 3;
		previousPage.setEnabled(false); // cannot go back to anything
		previousPage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pageCounter <= 1)
					return; // if somehow button is enabled at the beginning of the queriesList
				pageCounter--;

				pageStart = (pageCounter * 10) - 9; // 1,11,21,31,etc
				pageEnd = pageCounter * 10; // 10,20,30,40,etc
				if (pageEnd > wordResultsCount) pageEnd = wordResultsCount; //if the page ends before 10,20,30,etc
				pageLabel.setText("Showing page " + pageCounter + " of " + pages);
				resultHeaderLabel.setText(
						"Showing " + pageStart + " to " + pageEnd + " out of " + wordResultsCount + " results.");

				if (pageCounter == 1)
					previousPage.setEnabled(false); // cannot go back to anything
				else
					previousPage.setEnabled(true);
				if (pageCounter >= pages)
					nextPage.setEnabled(false); // cannot go forward to anything
				else
					nextPage.setEnabled(true);

				int i = 0;
				if (i == 0) { // result1
					if (pageStart + i > wordResultsCount)
						result.setText("There are no more results");
					else {
						result.setText(pageStart + i + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ " times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 0
				if (i == 1) { // result2
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 1
				if (i == 2) { // result3
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 2
				if (i == 3) { // result4
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 3
				if (i == 4) { // result5
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 4
				if (i == 5) { // result6
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 5
				if (i == 6) { // result7
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 6
				if (i == 7) { // result8
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 7
				if (i == 8) { // result9
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 8
				if (i == 9) { // result10
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 9
			}
		}); // end addActionListener
		frame.add(previousPage, c);

		c.gridy = 4;
		pageLabel.setSize(new Dimension(80, 40));
		frame.add(pageLabel, c);

		nextPage = new JButton("Next Page");
		c.gridy = 5;
		c.gridwidth = 1;
		nextPage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (pageCounter >= pages)
					return; // if somehow button is enabled at the end of the queriesList
				pageCounter++;

				pageStart = (pageCounter * 10) - 9; // 1,11,21,31,etc
				pageEnd = pageCounter * 10; // 10,20,30,40,etc
				if (pageEnd > wordResultsCount) pageEnd = wordResultsCount; //if the page ends before 10,20,30,etc
				pageLabel.setText("Showing page " + pageCounter + " of " + pages);
				resultHeaderLabel.setText(
						"Showing " + pageStart + " to " + pageEnd + " out of " + wordResultsCount + " results.");

				if (pageCounter == 1)
					previousPage.setEnabled(false); // cannot go back to anything
				else
					previousPage.setEnabled(true);
				if (pageCounter >= pages)
					nextPage.setEnabled(false); // cannot go forward to anything
				else
					nextPage.setEnabled(true);

				int i = 0;
				if (i == 0) { // result1
					if (pageStart + i > wordResultsCount)
						result.setText("There are no more results");
					else {
						result.setText(pageStart + i + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ " times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 0
				if (i == 1) { // result2
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 1
				if (i == 2) { // result3
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 2
				if (i == 3) { // result4
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 3
				if (i == 4) { // result5
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 4
				if (i == 5) { // result6
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 5
				if (i == 6) { // result7
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 6
				if (i == 7) { // result8
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 7
				if (i == 8) { // result9
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 8
				if (i == 9) { // result10
					if (pageStart + i <= wordResultsCount) {
						result.append("\n\n" + (pageStart + i) + ".\n"
								+ filenames.get(
										results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
								+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
								+ "times\n" + "In lines "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
										.toString()
								+ "\nLine "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
								+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
						i++; // increment i
					}
				} // end if i == 9
			}
		}); // end addActionListener
		frame.add(nextPage, c);

		// Initializing
		int i = 0;
		if (i == 0) { // result1
			if (pageStart + i > wordResultsCount)
				result.setText("There are no more results");
			else {
				result.setText(pageStart + i + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ " times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 0
		if (i == 1) { // result2
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 1
		if (i == 2) { // result3
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 2
		if (i == 3) { // result4
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 3
		if (i == 4) { // result5
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 4
		if (i == 5) { // result6
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 5
		if (i == 6) { // result7
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 6
		if (i == 7) { // result8
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 7
		if (i == 8) { // result9
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 8
		if (i == 9) { // result10
			if (pageStart + i <= wordResultsCount) {
				result.append("\n\n" + (pageStart + i) + ".\n"
						+ filenames.get(
								results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getFilenameIndex() -1)
						+ "\nAppears " + results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getCount()
						+ "times\n" + "In lines "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getWordLocations()
								.toString()
						+ "\nLine "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippetLocation() + ": "
						+ results.get(queries.get(queryCounter - 1)).get(pageStart + i - 1).getSnippet());
				i++; // increment i
			}
		} // end if i == 9

		c.gridy = 6;
		frame.add(resultHeaderLabel, c);
		
		c.gridy = 7;
		frame.add(recallLabel, c);
		
		c.gridy = 8;
		result.setEditable(false);
		result.setFont(new Font("Serif", Font.ITALIC, 16));
		
		JScrollPane scroll = new JScrollPane(result);
		scroll.setPreferredSize(new Dimension(900, 510));

		frame.add(scroll, c);
		frame.setResizable(false);
		frame.pack();
		frame.setVisible(true);
	}

	public static List<String> getFilenamesFromPremadeInvertedIndex(String PremadeInvertedIndex) throws IOException {
		List<String> filenames = new ArrayList<String>();
		List<String> lines = Files.readAllLines(Paths.get(PremadeInvertedIndex)); // reads all lines and turns a line
																					// into a single string
		for (String line : lines) {
			if (line.endsWith("html"))
				filenames.add(line);
			else
				break;
		} // end for

		return filenames;
	}

	public static Map<Integer, List<IndexItem>> parsePremadeInvertedIndex(String PremadeInvertedIndex,
			int filenamesCount) throws IOException {

		Map<Integer, List<IndexItem>> Index = new TreeMap<Integer, List<IndexItem>>(); // treemap for alphabetical order

		List<String> lines = Files.readAllLines(Paths.get(PremadeInvertedIndex)); // reads all lines and turns a line
																					// into a single string

		int lineCounter = 0;
		for (String line : lines) {
			if (lineCounter < filenamesCount) { // while the lineCounter is less than the filenamesCount
				lineCounter++; // it is still reading the .html filenames at the top of the
								// PremadeInvertedIndex
				continue; // so we can skip this line
			}

			IndexItem item = new IndexItem();
			int firstSpace = 0, secondSpace = 0, thirdSpace = 0;
			int counter = 0;

			while (true) { // goes through the line a character at a time
				if (line.length() <= counter)
					break; // exit loop once
				if (thirdSpace != 0)
					break;
				if (line.charAt(counter) == ' ') { // counter is at a space
					if (firstSpace == 0) // if firstSpace is 0 this means this is the first space encountered
						firstSpace = counter; // set firstSpace to the index
					else if (secondSpace == 0) // firstSpace was not 0 and secondSpace is 0, so this is the 2nd space
						secondSpace = counter;
					else if (thirdSpace == 0) // this is the third space
						thirdSpace = counter;
				}
				counter++; // increment counter
			}

			// end while
			item.setWord(line.substring(firstSpace + 1, secondSpace)); // characters between first space and second
																		// space is the word
																		// first space after html, +1 because includes
																		// space otherwise

			if (thirdSpace == 0) { // If thirdspace is 0, that is because there is no locations in premade inverted
									// index
				thirdSpace = line.length(); // set 3rd space to end of line
			} else {

				String temp = line.substring(thirdSpace + 1, line.length()); // creates a string holding the array of
																				// locations
				counter = 0; // reusing counter variable for new string
				boolean numStart = false;

				for (int i = 0; i < temp.length(); i++) {
					if (Character.isDigit(temp.charAt(i))) { // index is at a number
						if (!numStart) { // numStart is not in use, signifing searching for a new number
							numStart = true; // found the start of a number
							counter = i; // set counter to start of number index
						}
						// if numStart is true, means it has found a number, so any subsequent numbers
						// is part of the bigger number
					} else { // index is not at a number
						if (numStart) { // if it has found a number before
							// this means after the number is a comma, because array.toString()
							item.addWordLocation(Integer.parseInt(temp.substring(counter, i)));
							// creates a substring from the start of the number, and the current index
							// parses the substring into an integer, then adds it to the item location array
							numStart = false; // set numStart to false because now searching for a new number
						}
					} // end else index is not a number
				} // end for
			} // end else
			item.setCount(Integer.parseInt(line.substring(secondSpace + 1, thirdSpace)));
			// characters between 2nd space and 3rd space is the count of the word

			int fileindex = Integer.parseInt(line.substring(0, firstSpace));
			if (!Index.containsKey(fileindex)) { // the index does not have an entry for that filename
				Index.put(fileindex, new ArrayList<IndexItem>()); // create key with the filename in the index
			}
			Index.get(fileindex).add(item); // add the new IndexItem for that file
			// no matter what the index will have a key in the index

			lineCounter++;
		} // end for
		System.out.println("Done parsing PremadeInvertedIndex");

		return Index;
	} // end ParsePremadeIndex

	public static String getFileExtension(File file) {
		String fileName = file.getName(); // get filename
		if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			// if there is a . signaling a file extension at the end
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		// returns everything after the . such as 'html'
		else
			return ""; // else return blank
	}

	public static String parseTextFile(String fileName) throws IOException {
		String content = new String(Files.readAllBytes(Paths.get(fileName)));
		// converts the file into a string
		return content;
	}

	public static List<String[]> readTextFileByLines(String fileName) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(fileName)); // reads all lines and turns a line into a single
																		// string
		List<String[]> words = new ArrayList<String[]>();
		for (String line : lines) {
			String c[] = line.split(" "); // turn the single string into an array split on spaces, because of queries
											// file format
			words.add(c);
		}

		return words;
	}

	public static Map<Integer, List<IndexItem>> indexWebsite(List<String[]> content, List<String[]> sW, int fileIndex,
			Map<Integer, List<IndexItem>> InvertedIndex, boolean PortersAlgorithm) throws IOException {
		InvertedIndex.put(fileIndex, new ArrayList<IndexItem>()); // create index for the filename

		List<String> stopWords = new ArrayList<String>();
		for (String[] s : sW) {
			stopWords.add(s[0]); // add stopWords to its own List because the first list is a String[] because of
									// queries format
		}

		Map<String, IndexItem> words = new TreeMap<>();
		int lineCount = 1, wordCount = 0;

		for (String[] line : content) { // for each line in the content
			boolean notAWord = false;
			if (line.length == 0)
				notAWord = true; // blank line
			else {
				if (line[0].startsWith("<"))
					notAWord = true; // if starts with html tag bracket
				if (line[0].endsWith(">"))
					notAWord = true;
				; // if ends with html tag bracket
				if (line[0].endsWith(";"))
					notAWord = true; // if that line ends with ; is likely html
				if (line[0].startsWith("{"))
					notAWord = true; // if that line starts with { is likely html
			}

			if (notAWord) {
				lineCount = lineCount + 1; // increment contentCount by 1 because skipping that line
				continue;
			}

			for (String word : line) { // for each word in the line
				if (word.length() == 0)
					continue; // space, skip to next word
				if (word.length() <= 2)
					continue; // 2 characters or less, skip
				if (stopWords.contains(word))
					continue; // if the word is part of the StopWords, skip to next word
				if (word.length() > 45)
					continue; // if bigger than longest word then obviously is not a word
								// https://wordcounter.net/blog/2016/04/11/101421_what-is-the-longest-word.html

				if (word.startsWith("\""))
					word = word.substring(1, word.length()); // if the word starts with a quote, remove the quotation
																// mark
				if (word.endsWith("\""))
					word = word.substring(0, word.length() - 1); // if the word ends with a quote, remove the quotation
																	// mark.

				boolean letterOrDigit = true;
				if (Character.isLetter(word.charAt(0))) { // checks if the first character is a letter, if it is
															// continues to the for loop
					for (int i = 1; i < word.length(); i++) { // goes through all the characters of the word
						if (!Character.isLetterOrDigit(word.charAt(i))) // if encounters a non letter or digit
							letterOrDigit = false; // set the boolean flag to false
					} // end for
				} // end if
				else
					letterOrDigit = false; // the first character is not a letter, meaning this is not a proper "word"

				if (!letterOrDigit)
					continue; // if letterOrDigit is false, means this is not a proper word, skip the word

				if (PortersAlgorithm) { // if PortersAlgorithm flag is set to true
					Porter string = new Porter();
					word = string.stripAffixes(word); // runs the original word through the algorithm
				}

				if (words.containsKey(word)) { // if the Map already contains the word
					wordCount = words.get(word).getCount(); // get the word's count
					wordCount++; // increment +1
					words.get(word).setCount(wordCount); // set new count

					// snippet
					if (!words.get(word).getWordLocations().contains(lineCount)) { // if the array does not already
																					// contains the line
						words.get(word).addWordLocation(lineCount); // add the line
					}
				} // end containsWord if
				else {
					ArrayList<Integer> tempContentCount = new ArrayList<Integer>();
					tempContentCount.add(lineCount); // create an ArrayList<Integer> to store the first line
					words.put(word, new IndexItem(word, tempContentCount, 1)); // add new word, count is default 1 since
																				// first word
				} // end else - word not already in the map
			} // end for each word in line
			lineCount++; // increment line after reading all the words
		} // end for each line in content

		for (String key : words.keySet()) {
			InvertedIndex.get(fileIndex).add(words.get(key));
			// adds everything in the Words map to the InvertedIndex map
			// following the fileIndex, words, wordCount, wordLocations format
		}
		return InvertedIndex;
	}

	public static void saveInvertedIndex(Map<Integer, List<IndexItem>> InvertedIndex, List<String> filenames,
			boolean print, String filename) throws IOException {
		File fout = new File(filename);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

		Path file = Paths.get(filename);
		try {
			Files.deleteIfExists(file); // if the file exists delete it to override it.
		} catch (IOException e) {
		}

		// writes the filenames at the top of the inverted index file
		for (String fname : filenames) {
			bw.write(fname);
			bw.newLine();
			if (print)
				System.out.println(fname); // print
		}

		// then writes the actual inverted index items
		for (int index : InvertedIndex.keySet()) {
			int size = InvertedIndex.get(index).size();
			String item;
			for (int i = 0; i < size; i++) {
				item = ""; // reset item
				item += index; // filename index
				item += " " + InvertedIndex.get(index).get(i).getWord(); // word + space
				item += " " + InvertedIndex.get(index).get(i).getCount(); // count + space

				item += " " + InvertedIndex.get(index).get(i).getWordLocations().toString(); // locations

				bw.write(item);
				bw.newLine();
				if (print)
					System.out.println(item); // print
			} // end IndexItem
		} // end filename

		bw.close();
	}

	public static Map<String, List<IndexItem>> searchIndex(Map<Integer, List<IndexItem>> InvertedIndex,
			List<String> filenames, List<String[]> queryList, String resultLocation,
			String corpusDir, String output) throws IOException {
		List<String> results = new ArrayList<String>();
		// results is for the results.txt output text file

		Map<String, List<IndexItem>> resultsIndex = new TreeMap<String, List<IndexItem>>();
		// resultsIndex is for the search engine GUI
		// Map for results will have the key be the word itself, because each query is
		// its own word
		// the List<IndexItem> will now hold the filenameIndex, wordLocations,
		// wordCount, and the snippet

		for (String[] query : queryList) { // for every query in the querylist
			results.add(query[0] + " " + query[1]);
			boolean dupeQuery = false;

			if (resultsIndex.containsKey(query[1])) // If the query is already there, because both Query and Frequency
													// are in the Queries.txt for the term
				dupeQuery = true; // mark as true for later on
			else
				resultsIndex.put(query[1], new ArrayList<IndexItem>()); // the query is not in resultsIndex, so we add
																		// it first

			for (int filenameIndex : InvertedIndex.keySet()) { // goes through the inverted index for each file
				int size = InvertedIndex.get(filenameIndex).size();

				for (int i = 0; i < size; i++) { // goes through every indexed word for each file
					if (InvertedIndex.get(filenameIndex).get(i).getWord().equalsIgnoreCase(query[1])) {
						// checks if each word matches the query word
						String item = "";
						if (query[0].equalsIgnoreCase("query")) { // if query, all is wanted is the filename
							item = filenames.get(filenameIndex - 1);
						} else { // otherwise it will be frequency, and output filename + count
							item = filenames.get(filenameIndex - 1) + " "
									+ InvertedIndex.get(filenameIndex).get(i).getCount();
						}

						item += " " + InvertedIndex.get(filenameIndex).get(i).getWordLocations().toString();
						
						results.add(item);

						if (!dupeQuery) { // false = not in resultsIndex previously, so we index it in resultsIndex
							IndexItem wordMatch = new IndexItem();
							wordMatch.setFilenameIndex(filenameIndex);
							wordMatch.setWordLocations(InvertedIndex.get(filenameIndex).get(i).getWordLocations());
							wordMatch.setCount(InvertedIndex.get(filenameIndex).get(i).getCount());

							// snippet section, I am defining it as the longest line of the locations
							String website = filenames.get(filenameIndex - 1);

							List<String> websiteText = Files.readAllLines(Paths.get(corpusDir + "\\" + website));
							int lineCounter = 1, snippetLine = 0, locationCounter = 0;

							for (String line : websiteText) {
								if (wordMatch.getWordLocations().contains(lineCounter)) {
									if (snippetLine == 0) // first matching word location
										snippetLine = lineCounter; // set the snippetLine counter to the matching
																	// location
									else // 2+ matching word locations
									if (line.length() > websiteText.get(snippetLine).length()) // checking if the new
																								// matching location is
																								// longer than the old
																								// snippetLine
										snippetLine = lineCounter; // update snippetLine counter to new longest line
									locationCounter++; // went through one of the locations in the WordLocations array
								}
								lineCounter++;
								if (locationCounter > wordMatch.getWordLocations().size())
									break;
								// we have checked all the WordLocations so no need to check the rest of the
								// websiteText
							} // end for websiteText

							wordMatch.setSnippet(trimSnippetHTML(websiteText.get(snippetLine - 1))); // -1 to fix the +1
																										// on lineCount
							wordMatch.setSnippetLocation(snippetLine);
							resultsIndex.get(query[1]).add(wordMatch);							
						} // end if dupeQuery
						break; // break out because word only appears once no need to continue
					} // end if
				} // end for going through words

				if (resultsIndex.get(query[1]).isEmpty()) // there was no match for this website
					resultsIndex.remove(query[1]); // remove the website from the resultsIndex
			} // end for going through websites
			results.add("\n");
		} // end queryList
		
		if (output.equals("BOTH") || output.equals("TEXTFILE")) //GUI will not save results
		saveResults(results, resultLocation);
		// saveResults is here to be in the same scope as the results list

		return resultsIndex;
	}

	public static void saveResults(List<String> results, String filename) throws IOException {
		File fout = new File(filename);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

		Path file = Paths.get(filename);
		try {
			Files.deleteIfExists(file); // if the file exists delete it to override it.
		} catch (IOException e) {
		}
		for (String line : results) {
			bw.write(line);
			bw.newLine();
		}

		bw.close();
	}
}