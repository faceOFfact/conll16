import java.util.ArrayList;

import org.json.simple.*;


public class Word {
	JSONArray jword;
	String word,pos;
	int SentId = 0, DocId = 0;
	long st,en;
	TreeNode node;
	Sentence Sent;
	
	ArrayList<Link> links =  new ArrayList<Link>();
	boolean isConn=false;
	
	Word (int index, int i) {
		SentId = index; DocId = i;
	}
	
	void parseWord() {
		word = (String) jword.get(0);
		JSONObject info = (JSONObject) jword.get(1);
		st = (long) info.get("CharacterOffsetBegin");
		en = (long) info.get("CharacterOffsetEnd");
		pos = (String) info.get("PartOfSpeech");
		if (!parsing.PosSet.contains(pos)) {
			parsing.PosSet.add(pos);
			parsing.PosList.add(pos);
		}
		JSONArray linklist = (JSONArray) info.get("Linkers");
		String s="";
		for (int i=0; i<linklist.size(); ++i) {
			s = (String) linklist.get(i);
			String[] ss = s.split("_");
			Link link = new Link(ss[0],Integer.parseInt(ss[1]));
			links.add(link);
			if (ss[0].equals("conn")) isConn=true;
		}
	}
}
