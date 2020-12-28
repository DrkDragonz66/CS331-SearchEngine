import java.util.ArrayList;

public class IndexItem {
	String word, snippet;
	ArrayList<Integer> wordLocations;
	int count, filenameIndex, snippetLocation;
	
	IndexItem(){
		word = "";
		wordLocations = new ArrayList<Integer>();
		count = 0;
		snippet = "";
		filenameIndex = 0;
		snippetLocation = 0;
	}
	
	IndexItem(String word, int count){
		this.word = word;
		this.count = count;
	}
	
	IndexItem(String word, ArrayList<Integer> wordLocations, int count){
		this.word = word;
		this.wordLocations = wordLocations;
		this.count = count;
	}
	
	public String getWord() {
		return word;
	}
	
	public ArrayList<Integer> getWordLocations() {
		return wordLocations;
	}

	public int getCount() {
		return count;
	}
	
	public String getSnippet() {
		return snippet;
	}
	
	public int getFilenameIndex() {
		return filenameIndex;
	}
	public int getSnippetLocation() {
		return snippetLocation;
	}
	
	public void setWord(String word) {
		this.word = word;
	}
	
	public void addWordLocation(int location) {
		wordLocations.add(location);
	}
	
	public void setWordLocations(ArrayList<Integer> wordLocations) {
		this.wordLocations = wordLocations;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	
	public void setFilenameIndex(int filenameIndex) {
		this.filenameIndex = filenameIndex;
	}
	
	public void setSnippetLocation(int snippetLocation) {
		this.snippetLocation = snippetLocation;
	}
	
	public String toString() {
		return word + " " + count + " " + wordLocations.toString();
	}
	
}




