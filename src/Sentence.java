import java.util.ArrayList;

import org.json.simple.*;

public class Sentence {
	JSONObject sentence;
	ArrayList<Word> Words = new ArrayList<Word>();
	String Tree;
	int id = 0;
	TreeNode root = null;
	Doc doc;
	
	String poses[] = {"CC", "PP", "SBAR", "IN", "S", "VP", "NP", "VB", "TO", "DT", "JJ", "NN", "RB", "PRP", "MD", "ADJP", "ADVP"};
	
	Sentence(int index) {
		id = index;
	}
	
	boolean parseSent(boolean toBreak) {
		Tree = (String) sentence.get("parsetree");
		//if(toBreak)Tree = randomRemoveBracket(Tree);
		JSONArray words = (JSONArray) sentence.get("words");
		for (int i=0; i<words.size(); ++i) {
			Word word = new Word(i, Doc.wordcounter);
			Doc.wordcounter++;
			word.jword = (JSONArray) words.get(i);
			word.parseWord();
			word.Sent = this;
			Words.add(word);
		}
		return parseTree(toBreak);
//		for (int i=0; i<Words.size(); ++i) {
//			System.out.println(Words.get(i).node.content+"/"+Words.get(i).node.father.content+"/"+Words.get(i).node.father.father.content);
//		}
	}
	
	String randomRemoveBracket(String str){
		int leftBracket = 0;
		for(int i = 0;i < Tree.length();i++)
			if(str.charAt(i) == '(')leftBracket++;
		leftBracket = (int)(leftBracket*0.1) + 1;
		
		int removedLeft = 0;
		int removedRight = 0;
		StringBuffer s = new StringBuffer(str);
		for(int i = 0;i < s.length();i++)
			if(i > 0 && i < s.length()-3 && s.charAt(i) == '(' && (int)(1+Math.random()*100) < 50 && removedLeft < leftBracket){
				s.deleteCharAt(i);
				removedLeft++;
			}
			else if(i > 0 && i < s.length()-5 && s.charAt(i) == ')' && removedRight < removedLeft){
				s.deleteCharAt(i);
				removedRight++;
			}
		return s.toString();
	}
	
	boolean parseTree(boolean toBreak) {
		if (Tree.charAt(1)!=' ') return false;
		int count = 0;
		String s="";
		int i = 2;
		TreeNode fa = null;
		while (i<Tree.length()) {
			if (Tree.charAt(i)=='(') {
				s = "";
				for (int j=i+1; j<Tree.length(); ++j) {
					if (Tree.charAt(j)!=' ') s = s+Tree.charAt(j);
					else {
						i = j;
						break;
					}
				}
				TreeNode tn = new TreeNode(false);
				tn.content = s;
				
				if(toBreak){
					if((tn.content.equals("CC") || tn.content.equals("PP")) && (int)(1+Math.random()*100) <= 50){
						tn.content = "PP";
						parsing.aaaa++;
					}
					else if((int)(1+Math.random()*100) <= 33){
						tn.content = "PP";
						parsing.aaaa++;
					}
					//System.out.println(s+"   "+tn.content);
				}
				
				tn.father = fa;
				tn.Sent = this;
				if (fa == null){
					root = tn;
					tn.childIndex=0;
				} else {
					tn.childIndex = fa.children.size();
					fa.children.add(tn);
				}
				fa = tn;
			} else
			if (Tree.charAt(i)==')') {
                //Tree == "( )" shows the parsetree is empty
//				if ((i < Tree.length() - 2) || (count < Words.size())) System.out.println("parseTreeError!!");
				break;
			} else {
				s = "";
				for (int j=i; j<Tree.length(); ++j) {
					if (Tree.charAt(j)!=')') s = s+Tree.charAt(j);
					else {
						i = j;
						break;
					}
				}
				TreeNode tn = new TreeNode(true);
				tn.content = s;
				
				if(toBreak){
					if((tn.content.equals("CC") || tn.content.equals("PP")) && (int)(1+Math.random()*100) <= 50){
						tn.content = "PP";
						parsing.aaaa++;
					}
					else if((int)(1+Math.random()*100) <= 33){
						tn.content = "PP";
						parsing.aaaa++;
					}
					//System.out.println(s+"   "+tn.content);
				}
				
				tn.father = fa;
				tn.Sent = this;
				Words.get(count).node = tn;
				tn.word =  Words.get(count);
				count++;
				tn.childIndex = fa.children.size();
				fa.children.add(tn);
				fa = fa.father;
				++i;
				while (Tree.charAt(i)==')') {
					fa = fa.father;
					++i;
				}
			}
			++i;
		}
		return true;
	}
}
