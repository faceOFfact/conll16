import java.util.ArrayList;

import org.json.simple.*;


public class Doc {
	boolean istrain = true;
	JSONObject doc;
	int id = 0;
	int totalword = 0;
	ArrayList<Sentence> Sentences = new ArrayList<Sentence>();
	
	public static final int BOUND = 50;
	
	static int wordcounter = 0;
	
	Doc(int index) {
		id = index;
	}
	
	void parseDoc() {
		JSONArray sentences = (JSONArray) doc.get("sentences");
		wordcounter = 0;
		for (int i=0; i<sentences.size(); ++i ) {
			Sentence sent = new Sentence(i);
			sent.sentence = (JSONObject) sentences.get(i);
			int rdm = (int)(1+Math.random()*100);
			if (sent.parseSent(rdm<=BOUND?true:false)) {
				sent.doc = this;
				Sentences.add(sent);
			}
		}
		totalword = wordcounter;
	}
}
