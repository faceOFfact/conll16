import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import opennlp.maxent.BasicEventStream;
import opennlp.maxent.DataStream;
import opennlp.maxent.GIS;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.AbstractModelWriter;
import opennlp.model.EventStream;
import opennlp.model.GenericModelReader;
import opennlp.model.MaxentModel;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class parsing {
	public static ArrayList<Doc> Doclist = new ArrayList<Doc>(); 
	public static ArrayList<String> PosList = new ArrayList<String>();
	public static HashSet<String> PosSet = new HashSet<String>();
	public static ArrayList<String[]> connList = new ArrayList<String[]>();
	public static ArrayList<String> connSyntacList = new ArrayList<String>();
	public static ArrayList<Word> nodeDetected = new ArrayList<Word>();
	public static ArrayList<Integer> connDetected = new ArrayList<Integer>();
	public static HashMap<Integer, String> expSense = new HashMap<Integer, String>();
	public static ArrayList<String> senseResult = new ArrayList<String>();
	
	public static int aaaa = 0;
	public static int bbbb = 0;
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws IOException, ParseException {
		//Read
		BufferedReader br = new BufferedReader(new FileReader( new File("pdtb-parses-train.json")));
		String s = "";
		s = br.readLine();
		br.close();
		JSONParser jparser = new JSONParser();
		JSONObject jobj=(JSONObject) jparser.parse(s);
		for (Integer i=200; i<=2172; ++i) {
			String key = "wsj_";
			if (i<1000) key += "0";
			key += i.toString();
			if (jobj.keySet().contains(key)) {
				Doc doc = new Doc(i);
				doc.doc = (JSONObject) jobj.get(key);
				doc.parseDoc();
				doc.istrain = true;
				Doclist.add(doc);
				bbbb += doc.totalword;
			}
		}
		br = new BufferedReader(new FileReader( new File("pdtb-parses-dev.json")));
		s = "";
		s = br.readLine();
		br.close();
		jobj=(JSONObject) jparser.parse(s);
		for (Integer i=2200; i<=2282; ++i) {
			String key = "wsj_";
			key += i.toString();
			if (jobj.keySet().contains(key)) {
				Doc doc = new Doc(i);
				doc.doc = (JSONObject) jobj.get(key);
				doc.parseDoc();
				doc.istrain = false;
				Doclist.add(doc);
			}
		}
		
		//Connective Detection: feature construction
		br = new BufferedReader(new FileReader( new File("connective.txt")));
		while ((s=br.readLine()) != null) {
			String[] tmp = s.split("\t");
			connSyntacList.add(tmp[1]);
			String[] ss = tmp[0].split(" ");
			connList.add(ss);
		}
		br.close();	
		
		buildAllSenseSet();
		buildnonExpSenseSet();
		buildExpSense();
		
		PrintWriter pw1 = new PrintWriter(new FileWriter(new File("detection_train.txt")));
		PrintWriter pw2 = new PrintWriter(new FileWriter(new File("detection_test.txt")));
		PrintWriter pw3 = new PrintWriter(new FileWriter(new File("sense_train.txt")));
		PrintWriter pw4 = new PrintWriter(new FileWriter(new File("sense_test.txt")));
		PrintWriter pw;
		int posi=0,nega=0,ooo=0;
		boolean changed = false;
		for (int docId=0; docId<Doclist.size(); ++docId) {
			Doc doc = Doclist.get(docId);
			for (int sentId=0; sentId<doc.Sentences.size(); ++sentId) {
				Sentence sent = doc.Sentences.get(sentId);
				for (int wordId=0; wordId<sent.Words.size(); ++wordId) {
					Word word = sent.Words.get(wordId);
					for (int connId=0; connId<connList.size(); ++connId) {
						changed = false;
						String[] conn = connList.get(connId);
//						if (conn.length>1) continue;
						int le = 0;
						boolean check = true;
						while (le<conn.length) {
							if ((wordId+le)<sent.Words.size()) {
								Word tmpW = sent.Words.get(wordId+le);
								if (tmpW.word.toLowerCase().equals(conn[le])) {
									le++;
									continue;
								}
							}
                            //check - whether exists a conn in the sentence
							check = false;
							break;
						}
						if (check) {
                            //pw1 - training set
                            //pw2 - testing set
							if (doc.istrain) pw = pw1;
							else pw = pw2;
							String connText = conn[0];
							/*************************added*************************/
							/*
							if(wordId > 0 && timeWord.contains(sent.Words.get(wordId-1).word)){
								changed = true;
								//connText = sent.Words.get(wordId-1).word + "_" + connText;
								System.out.print(connText+" ");
								
							}
							*/
							for (int i=1; i<conn.length; ++i) {
								connText = connText + "_" + conn[i];
							}
							
							//C POS
							pw.print("ConnPos=");
							pw.print(connText+"_");
							if (conn.length>1) pw.print("PP");
							else pw.print(word.pos);
							pw.print(" ");
							
							//prev + C
							pw.print("PrevConn=");
							if (wordId == 0) pw.print("null_");
							else pw.print(sent.Words.get(wordId-1).word+"_");
							pw.print(connText+" ");
							
							//prev POS
							pw.print("PrevPos=");
							if (wordId == 0) pw.print("null_null ");
							else pw.print(sent.Words.get(wordId-1).word+"_"+sent.Words.get(wordId-1).pos+" ");
							
							//prev POS + C POS
							pw.print("PrevPosConnPos=");
							if (wordId == 0) pw.print("null_null_");
							else pw.print(sent.Words.get(wordId-1).word+"_"+sent.Words.get(wordId-1).pos+"_");
							pw.print(connText+"_");
							if (conn.length>1) pw.print("PP");
							else pw.print(word.pos);
							pw.print(" ");
							
							//C + next
							pw.print("ConnNext=");
							pw.print(connText+"_");
							if ((wordId+le)==sent.Words.size()) pw.print("null ");
							else pw.print(sent.Words.get(wordId+le).word+" ");
							
							//next POS
							pw.print("NextPos=");
							if ((wordId+le)==sent.Words.size()) pw.print("null_null ");
							else pw.print(sent.Words.get(wordId+le).word+"_"+sent.Words.get(wordId+le).pos+" ");
							
							//C POS + next POS
							pw.print("ConnPosNextPos=");
							pw.print(connText+"_");
							if (conn.length>1) pw.print("PP");
							else pw.print(word.pos);
							pw.print("_");
							if ((wordId+le)==sent.Words.size()) pw.print("null_null ");
							else pw.print(sent.Words.get(wordId+le).word+"_"+sent.Words.get(wordId+le).pos+" ");							
							
							//label
                            //whether this word/phrase ever functioned as conn in the gold standard relations
							boolean bo = word.isConn;
							le = 0;
							while (le < conn.length) {
								word = sent.Words.get(wordId+le);
								if (word.isConn==false) {
									bo = false;
									break;
								}
								le++;
							}
							if (bo) {
								posi++;
								
								//if(changed)pw.println("0");
								//else 
									pw.println("1");
									
								nodeDetected.add(sent.Words.get(wordId));
								connDetected.add(connId);
								
								if(doc.istrain){
									boolean existExp = false;
									String toPrint = new String();
									word = sent.Words.get(wordId);
									for(int linkId = 0;linkId < word.links.size();linkId++){
										if(expSense.get((Integer)word.links.get(linkId).linknum) != null){
											toPrint = expSense.get((Integer)word.links.get(linkId).linknum);
											existExp = true;
											break;
										}
									}
									if(existExp){
										//C string
										pw3.print("Cstring="+connText+" ");
										//C POS
										if(conn.length > 1)pw3.print("CPOS="+word.node.father.father.content+" ");
										else pw3.print("CPOS="+word.pos+" ");
										//prev + C
										pw3.print("Cprev="+connText);
										if (wordId == 0) pw3.print("_null"+" ");
										else pw3.print("_"+sent.Words.get(wordId-1).word+" ");
										
										TreeNode currentNode = word.node.father;
										if(conn.length > 1)currentNode = currentNode.father;
										boolean noFa = false;
										if(currentNode.father == null)noFa = true;
										//System.out.println(currentNode.father.content+" "+word.word);
										
										//parent category
										if(noFa)pw3.print("PC=null ");
										else pw3.print("PC="+currentNode.father.content+" ");
										//left sibling category
										pw3.print("LSC=");
										if(noFa || currentNode.childIndex == 0)pw3.print("null ");
										else pw3.print(currentNode.father.children.get(currentNode.childIndex-1).content+" ");
										//right sibling category
										pw3.print("RSC=");
										if(noFa || currentNode.childIndex == currentNode.father.children.size()-1)pw3.print("null ");
										else pw3.print(currentNode.father.children.get(currentNode.childIndex+1).content+" ");
										
										pw3.println(toPrint);
									}
								}
								else{
									//C string
									pw4.print("Cstring="+connText+" ");
									//C POS
									if(conn.length > 1)pw4.print("CPOS="+word.node.father.father.content+" ");
									else pw4.print("CPOS="+word.pos+" ");
									//prev + C
									pw4.print("Cprev="+connText);
									if (wordId == 0) pw4.print("_null");
									else pw4.print("_"+sent.Words.get(wordId-1).word);
									
									TreeNode currentNode = word.node.father;
									if(conn.length > 1)currentNode = currentNode.father;
									boolean noFa = false;
									if(currentNode.father == null)noFa = true;
									
									//parent category
									if(noFa)pw4.print("PC=null ");
									else pw4.print("PC="+currentNode.father.content+" ");
									//left sibling category
									pw4.print("LSC=");
									if(noFa || currentNode.childIndex == 0)pw4.print("null ");
									else pw4.print(currentNode.father.children.get(currentNode.childIndex-1).content+" ");
									//right sibling category
									pw4.print("RSC=");
									if(noFa || currentNode.childIndex == currentNode.father.children.size()-1)pw4.print("null ");
									else pw4.print(currentNode.father.children.get(currentNode.childIndex+1).content+" ");
									
									pw4.println();
								}
							}
							else {
								nega++;
								pw.println("0");
							}
							break;
						}
					}
				}
			}
		}
		System.out.println(ooo);
		System.out.println(posi+" "+nega+" "+(posi+nega));
		pw1.close();
		pw2.close();
		pw3.close();
		pw4.close();
		
		//Connective Detection: training and prediction
		EventStream Detection_train = new BasicEventStream(new PlainTextByLineDataStream(new FileReader(new File("detection_train.txt"))));
        AbstractModel Detect_Model;
        Detect_Model = GIS.trainModel(Detection_train);
        AbstractModelWriter writer;
        writer = new SuffixSensitiveGISModelWriter(Detect_Model, new File("detection_model.txt"));
        writer.persist();
		
        MaxentModel m = new GenericModelReader(new File("detection_model.txt")).getModel();
        Predict pre = new Predict(m);
        DataStream ds = new PlainTextByLineDataStream(new FileReader(new File("detection_test.txt")));
        double tp=0, tn=0, fp=0, fn=0;
        while (ds.hasNext()) {
        	s = (String) ds.nextToken();
        	String out = pre.eval(s.substring(0, s.lastIndexOf(' ')));
        	String label = s.substring(s.length()-1, s.length());
        	if (out.equals("1") && label.equals("1")) tp++;
        	if (out.equals("1") && label.equals("0")) fp++;
        	if (out.equals("0") && label.equals("1")) fn++;
        	if (out.equals("0") && label.equals("0")) tn++;
        }
    	double pr = tp / (tp+fp);
    	double re = tp / (tp+fn);
    	double f1 = 2*pr*re/(pr+re);
    	double ac = (tp+tn)/(tp+fp+fn+tn);
    	System.out.println("tp="+tp+"\ttn="+tn+"\tfp="+fp+"\tfn="+fn);
    	System.out.println("detection prcision=\t"+pr);
    	System.out.println("detection recall=\t"+re);
    	System.out.println("detection f1=\t\t"+f1);
    	System.out.println("detection accuracy=\t"+ac);
    	System.out.println("detection accuracy=\t"+aaaa);
    	System.out.println("detection accuracy=\t"+bbbb);
    	/*
    	//Connective Sense: training and prediction
		EventStream Sense_train = new BasicEventStream(new PlainTextByLineDataStream(new FileReader(new File("sense_train.txt"))));
        AbstractModel Sense_Model;
        Sense_Model = GIS.trainModel(Sense_train);
        writer = new SuffixSensitiveGISModelWriter(Sense_Model, new File("sense_model.txt"));
        writer.persist();
		
        m = new GenericModelReader(new File("sense_model.txt")).getModel();
        pre = new Predict(m);
        ds = new PlainTextByLineDataStream(new FileReader(new File("sense_test.txt")));
        /*
        while (ds.hasNext()) {
        	s = (String) ds.nextToken();
        	String out = pre.eval(s);
        	String checkConn = s.substring(s.indexOf('=')+1, s.indexOf(' '));
        	if(out.equals("Expansion"))out = "Expansion.Conjunction";
			if(out.equals("Comparison"))out = "Comparison.Contrast";
			if(out.equals("Temporal"))out = "Temporal.Synchrony";
			if(out.equals("alternative"))out = "Expansion.Alternative.Chosen alternative";
			if(out.equals("Temporal.Asynchronous"))out = "Temporal.Asynchronous.Precedence";
			
        	if(checkConn.equals("for_example") || checkConn.equals("for_instance"))senseResult.add("Expansion.Instantiation");
        	else senseResult.add(out);
        }
        System.out.println("number of explicit connectives:\t"+senseResult.size());
    	
        
    	//Argument Extraction: feature construction
		pw1 = new PrintWriter(new FileWriter(new File("extraction_train.txt")));
		pw2 = new PrintWriter(new FileWriter(new File("extraction_test.txt")));
    	for (int nodeId=0; nodeId<nodeDetected.size(); nodeId++) {
            //the lengths of nodeDetected and connDetected are identical
            //curWord refers to the first word in the conn
            //conn refers to the whole phrase/word of conn
    		Word curWord = nodeDetected.get(nodeId);
    		String[] conn = connList.get(connDetected.get(nodeId));
			if (curWord.Sent.doc.istrain) pw = pw1;
			else pw = pw2;
//    		if (conn.length>1) {
//    			System.out.println(curWord.Sent.doc.id);
//    			for (int i=0; i<conn.length;++i) System.out.println(conn[i]);
//    		}
    		candidates.clear();
    		LeftRight.clear();
    		TreeNode connNode = findNode(curWord,conn);
    		String connText = conn[0];
			for (int i=1; i<conn.length; ++i) {
				connText = connText + "_" + conn[i];
			}
//			if (conn.length>1) {
//				System.out.println(connNode.content+" "+connNode.children.size());
//				for (int i=0; i<connNode.children.size(); ++i) {
//					System.out.println(connNode.children.get(i).content+" "+connNode.children.get(i).children.size());
//				}
//				System.out.println(candidates.size());
//				for (int i=0; i<candidates.size(); ++i) {
//					System.out.println(candidates.get(i).content+" "+candidates.get(i).children.size());
//				}
//			}
			
			String connFeature = "";
			if (connNode.father == null) {
				continue;
			}
			connFeature += "ConStr=" + curWord.word.charAt(0) + connText.substring(1, connText.length()) + " ";
			connFeature += "ConLStr=" + connText + " ";
			connFeature += "ConCat=" + connSyntacList.get(connDetected.get(nodeId)) + " ";
			connFeature += "ConLSib=" + connNode.childIndex + " ";
			connFeature += "ConRSib=" + (connNode.father.children.size()-1-connNode.childIndex) + " ";
			
			//Lower nodes
			for (int i=0; i<candidates.size(); ++i) {
				//CandiCtx
				String candFeature = "CandiCtx=";
				TreeNode candi = candidates.get(i);
				candFeature += candi.content + "-";
				if (candi.father != null) candFeature += candi.father.content + "-";
				else candFeature += "NULL-";
				if (candi.childIndex == 0) candFeature += "NULL-";
				else candFeature += candi.father.children.get(candi.childIndex-1).content + "-";
				if (candi.childIndex == candi.father.children.size()-1) candFeature += "NULL ";
				else candFeature += candi.father.children.get(candi.childIndex+1).content + " ";
				
				//ConCandiPath
				TreeNode tmp = candi;
				String path = tmp.content;
				while (!tmp.equals(connNode.father)) {
					tmp = tmp.father;
					path = tmp.content+"<"+path;
				}
				candFeature += "ConCandiPath=" + path + " ";
				
				//ConCandiPosition
				candFeature += "ConCandiPosi=" + LeftRight.get(i) + " ";
				
				//ConCandiPathLSib
				candFeature += "ConCandiPathLSib=" + path + ":";
				if (connNode.childIndex>1) candFeature += "]1 ";
				else candFeature += "[1 ";
				
				//Label
				int connId = 0;
				for (int j=0; j<curWord.links.size(); ++j) {
					if (curWord.links.get(j).linktype.equals("conn")) {
						connId = curWord.links.get(j).linknum;
						break;
					}
				}
				String label = Check(connId, candi);
				if (label=="") label = "null";
				
				//Combine
//				pw.println("L: "+candi.content+" "+candi.children.size());
				pw.println(connFeature+candFeature+label);
				if (!curWord.Sent.doc.istrain) {
					connInTest.add(connNode);
					candiInTest.add(candi);
					if (conn.length>1) {
						connST.add(stID);
						connEN.add(enID);
						String temp = "";
						for(int j=0; j<conn.length; ++j){
							if(j == conn.length - 1)temp = temp + conn[j];
							else temp = temp + conn[j] + " ";
							connStr.add(temp);
						}
					} else {
						connST.add(curWord.DocId);
						connEN.add(curWord.DocId);
						connStr.add(curWord.word);
					}
				}
			}
			
			//Higer nodes
			TreeNode curN = connNode;
			while (curN.father != null) {
				int ind = curN.childIndex;
				curN = curN.father;
//				System.out.println(curN.content+" "+curN.children.size()+" "+ind);
				for (int i=0; i<curN.children.size(); ++i) 
					if (i != ind) {
						//CandiCtx
                        //the combination of the constituent, its parent, left and right sibling
						String candFeature = "CandiCtx=";
						TreeNode candi = curN.children.get(i);
						candFeature += candi.content + "-";
						candFeature += candi.father.content + "-";
						if (candi.childIndex == 0) candFeature += "NULL-";
						else candFeature += curN.children.get(candi.childIndex-1).content + "-";
						if (candi.childIndex == candi.father.children.size()-1) candFeature += "NULL ";
						else candFeature += curN.children.get(candi.childIndex+1).content + " ";
			
						//ConCandiPath
                        //the path from the parent node of the conn to the constituent
						TreeNode tmp = connNode.father;
						String path = tmp.content;
						while (!tmp.equals(curN)) {
							tmp = tmp.father;
							path = path + ">"+ tmp.content;
						}
						path += "<" + candi.content;
						candFeature += "ConCandiPath=" + path + " ";
						
						//ConCandiPosition
                        //the position of constituent relative to the conn
						if (i<ind) candFeature += "ConCandiPosi=left ";
						else candFeature += "ConCandiPosi=right ";
						
						//ConCandiPathLSib
                        //ConCandiPath + whether the number of left sibling of conn is greater than 1
						candFeature += "ConCandiPathLSib=" + path + ":";
						if (connNode.childIndex>1) candFeature += "]1 ";
						else candFeature += "[1 ";
						
						//Label
						int connId = 0;
						for (int j=0; j<curWord.links.size(); ++j) {
							if (curWord.links.get(j).linktype.equals("conn")) {
								connId = curWord.links.get(j).linknum;
								break;
							}
						}
						String label = Check(connId, candi);
						if (label=="") label = "null";
						
						//Combine
//						pw.println("H: "+candi.content+" "+candi.children.size());
						pw.println(connFeature+candFeature+label);
						if (!curWord.Sent.doc.istrain) {
							connInTest.add(connNode);
							candiInTest.add(candi);
							if (conn.length>1) {
								connST.add(stID);
								connEN.add(enID);
								String temp = "";
								for(int j=0; j<conn.length; ++j){
									if(j == conn.length - 1)temp = temp + conn[j];
									else temp = temp + conn[j] + " ";
									connStr.add(temp);
								}
							} else {
								connST.add(curWord.DocId);
								connEN.add(curWord.DocId);
								connStr.add(curWord.word);
							}
						}
					}
			}
			
			//Previous sentence
			if (curWord.Sent.id > 0) {
				curN = curWord.Sent.doc.Sentences.get(curWord.Sent.id-1).root;
				//CandiCtx
				String candFeature = "CandiCtx=";
				TreeNode candi = curN;
				candFeature += candi.content + "-";
				candFeature += "NULL-NULL-NULL ";
	
				//ConCandiPath
				TreeNode tmp = connNode.father;
				String path = tmp.content;
				//path = connNode.content + path;
				while (!(tmp.father == null)) {
					tmp = tmp.father;
					path = path + ">"+ tmp.content;
				}
				path += ">DOC<" + candi.content;
				candFeature += "ConCandiPath=" + path + " ";
				
				//ConCandiPosition
				candFeature += "ConCandiPosi=previous ";
				
				//ConCandiPathLSib
				candFeature += "ConCandiPathLSib=" + path + ":";
				if (connNode.childIndex>1) candFeature += "]1 ";
				else candFeature += "[1 ";
				
				//Label
				int connId = 0;
				for (int j=0; j<curWord.links.size(); ++j) {
					if (curWord.links.get(j).linktype.equals("conn")) {
						connId = curWord.links.get(j).linknum;
						break;
					}
				}
				String label = Check(connId, candi);
				if (label=="") label = "null";
				
				//Combine
//				pw.println("P: "+candi.content+" "+candi.children.size());
				pw.println(connFeature+candFeature+label);
				if (!curWord.Sent.doc.istrain) {
					connInTest.add(connNode);
					candiInTest.add(candi);
					if (conn.length>1) {
						connST.add(stID);
						connEN.add(enID);
						String temp = "";
						for(int j=0; j<conn.length; ++j){
							if(j == conn.length - 1)temp = temp + conn[j];
							else temp = temp + conn[j] + " ";
							connStr.add(temp);
						}
					} else {
						connST.add(curWord.DocId);
						connEN.add(curWord.DocId);
						connStr.add(curWord.word);
					}
				}
			}			
    	}
    	pw1.close();
    	pw2.close();
    	
    	//Argument Extraction: training and prediction
    	pw = new PrintWriter(new FileWriter(new File("output_Exp.json")));
    	EventStream Extraction_train = new BasicEventStream(new PlainTextByLineDataStream(new FileReader(new File("extraction_train.txt"))));
        AbstractModel Extraction_Model;
        Extraction_Model = GIS.trainModel(Extraction_train);
        writer = new SuffixSensitiveGISModelWriter(Extraction_Model, new File("extraction_model.txt"));
        writer.persist();
		
        m = new GenericModelReader(new File("extraction_model.txt")).getModel();
        pre = new Predict(m);
        ds = new PlainTextByLineDataStream(new FileReader(new File("extraction_test.txt")));
        Integer total = 0, hit = 0;
        TreeNode connT = null;
        ArrayList<Integer> arg1 = new ArrayList<Integer>();
        ArrayList<Integer> arg2 = new ArrayList<Integer>();
        initSenseList();
        int i = 0;
        int senseNum = 0;
        while (ds.hasNext()) {
        	s = (String) ds.nextToken();
        	String out = pre.eval(s.substring(0, s.lastIndexOf(' ')));
        	String label = s.substring(s.lastIndexOf(' ')+1, s.length());
        	if (out.equals(label)) {
        		hit++;
        	}
        	total++;
        	if (connT == null) {
        		connT = connInTest.get(i);
        		++i;
        		continue;
        	}
        	if (!connT.equals(connInTest.get(i))) {
        		Collections.sort(arg1);
        		Collections.sort(arg2);
        		JSONObject job = new JSONObject();
        		JSONObject Jarg = new JSONObject();
        		JSONArray arr = new JSONArray();
        		for (int j=0; j<arg1.size();++j) {
        			arr.add(arg1.get(j));
        		}
        		Integer arg1Last = -1;
        		if(!arg1.isEmpty())arg1Last = arg1.get(arg1.size()-1);
        		Jarg.put("TokenList", arr);
        		job.put("Arg1", Jarg);
        		
        		Jarg = new JSONObject();
        		arr = new JSONArray();
        		for (int j=0; j<arg2.size();++j) {
        			arr.add(arg2.get(j));
        		}
        		Integer arg2First = -1;
        		if(!arg2.isEmpty())arg2First = arg2.get(0);
        		Jarg.put("TokenList", arr);
        		job.put("Arg2", Jarg);
        		
        		//arg1.clear();
        		//addArg(arg1,connT);
        		Jarg = new JSONObject();
        		arr = new JSONArray();
        		for (int j=connST.get(i-1); j<=connEN.get(i-1);++j) {
        			arr.add(j);
        		}
        		Jarg.put("TokenList", arr);
        		job.put("Connective", Jarg);
        		job.put("DocID", "wsj_"+connT.Sent.doc.id);
        		//System.out.println(arg1Last.toString()+" "+arg2First.toString());
        		//store the docID+preSent+curSent tuple
        		//don't use this function temporarily because the argument labeling of a conn is not synchronous
        		
        		if(arg1Last != -1 && arg2First != -1){
	        		int arg1Pos = getSentIDOfWord(arg1Last, (Integer)connT.Sent.doc.id);
	        		int arg2Pos = getSentIDOfWord(arg2First, (Integer)connT.Sent.doc.id);
	        		//System.out.println(((Integer)arg1Pos).toString()+" "+((Integer)arg2Pos).toString()+" "+((Integer)connT.Sent.doc.id).toString());
	        		if(arg1Pos != arg2Pos){
	        			if(preSentArg.get("wsj_"+connT.Sent.doc.id) == null){
	        				ArrayList<Integer>tmp = new ArrayList<Integer>();
	        				tmp.add(arg1Pos);
	        				String str = "wsj_"+connT.Sent.doc.id;
	        				preSentArg.put(str, tmp);
	        			}
	        			else{
	        				preSentArg.get("wsj_"+connT.Sent.doc.id).add(arg1Pos);
	        			}
	        			//preSentArg.put("wsj_"+connT.Sent.doc.id, tmp);
	        		}
        		}
        		
        		arr = new JSONArray();
        		arr.add(senseResult.get(senseNum++));
        		job.put("Sense", arr);
        		job.put("Type", "Explicit");
        		pw.println(job.toJSONString());
        		
        		arg1.clear();
        		arg2.clear();        		
        		connT = connInTest.get(i);
        	} 
        	if (out.equals("arg1")) {
        		addArg(arg1, candiInTest.get(i));
        	} 
        	else if (out.equals("arg2")) {
        		addArg(arg2, candiInTest.get(i));
        	}
        	++i;
        }
        Collections.sort(arg1);
		Collections.sort(arg2);
		JSONObject job = new JSONObject();
		JSONObject Jarg = new JSONObject();
		JSONArray arr = new JSONArray();
		for (int j=0; j<arg1.size();++j) {
			arr.add(arg1.get(j));
		}
		Integer arg1Last = -1;
		if(!arg1.isEmpty())arg1Last = arg1.get(arg1.size()-1);
		Jarg.put("TokenList", arr);
		job.put("Arg1", Jarg);
		
		Jarg = new JSONObject();
		arr = new JSONArray();
		for (int j=0; j<arg2.size();++j) {
			arr.add(arg2.get(j));
		}
		Integer arg2First = -1;
		if(!arg2.isEmpty())arg2First = arg2.get(0);
		Jarg.put("TokenList", arr);
		job.put("Arg2", Jarg);
		arg1.clear();
		
		//addArg(arg1,connT);
		Jarg = new JSONObject();
		arr = new JSONArray();
		for (int j=connST.get(i-1); j<=connEN.get(i-1);++j) {
			arr.add(j);
		}
		Jarg.put("TokenList", arr);
		job.put("Connective", Jarg);
		job.put("DocID", "wsj_"+connT.Sent.doc.id);
		
		if(arg1Last != -1 && arg2First != -1){
    		int arg1Pos = getSentIDOfWord(arg1Last, (Integer)connT.Sent.doc.id);
    		int arg2Pos = getSentIDOfWord(arg2First, (Integer)connT.Sent.doc.id);
    		//System.out.println(((Integer)arg1Pos).toString()+" "+((Integer)arg2Pos).toString()+" "+((Integer)connT.Sent.doc.id).toString());
    		if(arg1Pos != arg2Pos){
    			if(preSentArg.get("wsj_"+connT.Sent.doc.id) == null){
    				ArrayList<Integer>tmp = new ArrayList<Integer>();
    				tmp.add(arg1Pos);
    				String str = "wsj_"+connT.Sent.doc.id;
    				preSentArg.put(str, tmp);
    			}
    			else{
    				preSentArg.get("wsj_"+connT.Sent.doc.id).add(arg1Pos);
    			}
    			//preSentArg.put("wsj_"+connT.Sent.doc.id, tmp);
    		}
		}
		
		arr = new JSONArray();
		arr.add(senseResult.get(senseNum++));
		//System.out.println(connStr.get(i-1));
		job.put("Sense", arr);
		job.put("Type", "Explicit");
		pw.println(job.toJSONString());
		pw.close();
		
		System.out.println("sense got:\t"+senseNum);
        System.out.println("extraction accuracy=\t"+(hit.doubleValue()/total.doubleValue()));
        System.out.println(connInTest.size()+" "+candiInTest.size()+" "+connST.size()+" "+connEN.size());
        
        
        //construct gold set for non-explicit relations
        System.out.println("constructing nonExp gold set");
        buildNonExpGoldSpan();
        buildParaEnd();
        
        //nonExp: extract features in the training set
        pw1 = new PrintWriter(new FileWriter(new File("nonExp_train.txt")));
		pw2 = new PrintWriter(new FileWriter(new File("nonExp_test.txt")));
		PrintWriter pw5 = new PrintWriter(new FileWriter(new File("nonExpTmp.json")));
		int count = 0;
		//int count2 = 0;
		for (int docId=0; docId<Doclist.size(); ++docId) {
			Doc doc = Doclist.get(docId);
			if(doc.istrain)
				pw = pw1;
			else
				pw = pw2;
			boolean hasPreSentArg = false;
			String key = "wsj_";
			if (doc.id<1000) key += "0";
			key += ((Integer)doc.id).toString();
			
			//if the doc is in train set but without a nonExp sense, just skip
			if(doc.istrain && !nonExpGoldDoc.contains(key))continue;
			
			if(preSentArg.containsKey(key))
				hasPreSentArg = true;
			//System.out.println(key);
			//continue;
			
			for (int sentId=0; sentId<doc.Sentences.size(); ++sentId) {
				//count++;
				//ignore the sentences at the end of a paragraph
				if(sentId == doc.Sentences.size()-1)continue;
				//ignore the sentences which had functioned as arg1 in the test set
				//ignore the sentences at the end of a paragraph
				String wholeSent = new String();
				for(int wordId = 0;wordId < doc.Sentences.get(sentId).Words.size();wordId++)
					wholeSent += doc.Sentences.get(sentId).Words.get(wordId).word;
				if(wholeSent.length() >= 30)wholeSent = wholeSent.substring(wholeSent.length()-25, wholeSent.length()-5);
				wholeSent = wholeSent.toLowerCase().replaceAll("[\\pP\\p{Punct}]", "");
				//if(key.equals("wsj_0300"))
					//System.out.println(wholeSent);
				
				if(!doc.istrain){
					if(hasPreSentArg && preSentArg.get(key).contains(sentId))
						continue;

					boolean check = false;
					if(paraEnd.get(key) != null){
						ArrayList<String>tmp = paraEnd.get(key);
						for(int segId = 0;segId < tmp.size();segId++)
							if(tmp.get(segId).contains(wholeSent)){
								check = true;
								break;
							}
						if(paraEnd.containsKey(key) && check){
							count++;
							continue;
						}
					}
				}
				
				Sentence sent1 = doc.Sentences.get(sentId);
				Sentence sent2 = doc.Sentences.get(sentId+1);
				
				arg1.clear();
				arg2.clear();
				//get the spans of two sentences, storing in arg1/2
				//if(!doc.istrain){
				for(int wordId=0; wordId<sent1.Words.size()-1; ++wordId) {
					arg1.add(sent1.Words.get(wordId).DocId);
				}
				for(int wordId=0; wordId<sent2.Words.size()-1; ++wordId) {
					arg2.add(sent2.Words.get(wordId).DocId);
				}

				int sent1first, sent1last, sent2first, sent2last;
				sent1first = sent1.Words.get(0).DocId;
				if(sent1.Words.size() < 2)sent1last = sent1first;
				else sent1last = sent1.Words.get(sent1.Words.size()-2).DocId;
				sent2first = sent2.Words.get(0).DocId;
				if(sent2.Words.size() < 2)sent2last = sent2first;
				else sent2last = sent2.Words.get(sent2.Words.size()-2).DocId;
				//System.out.println(doc.id+" "+sent1first+" "+sent1last+" "+sent2first+" "+sent2last);
				//break;
				//if(doc.istrain)count1++;
				
				boolean toExtract = false;
				String goldSense = "Temporal.Asynchronous.Succession";
				for(int goldId = 0;goldId < nonExpGoldDoc.size();goldId++){
					if(nonExpGoldDoc.get(goldId).equals(key)){
						if((nonExpGoldSpan1.get(goldId).contains(sent1first) || nonExpGoldSpan1.get(goldId).contains(sent1last))
						&& (nonExpGoldSpan2.get(goldId).contains(sent2first) || nonExpGoldSpan2.get(goldId).contains(sent2last))){
							toExtract = true;
							//count2++;
							if(nonExpSenseSet.contains(nonExpGoldSense.get(goldId)))goldSense = nonExpGoldSense.get(goldId);
							//if(goldSense.equals("alternative"))System.out.println(goldSense);
							break;
						}
					}
				}
				if(!doc.istrain)toExtract = true;
				
				if(toExtract){
					String Arg1Last = new String();
					String Arg2Last = new String();
					if(sent1.Words.size() > 2)Arg1Last = sent1.Words.get(sent1.Words.size()-2).word;
					else Arg1Last = "NULL";
					if(sent2.Words.size() > 2)Arg2Last = sent2.Words.get(sent2.Words.size()-2).word;
					else Arg2Last = "NULL";
					pw.print("Arg1Last="+Arg1Last+" ");
					pw.print("Arg1First="+sent1.Words.get(0).word+" ");
					pw.print("Arg2Last="+Arg2Last+" ");
					pw.print("Arg2First="+sent2.Words.get(0).word+" ");
					pw.print("FirstS="+sent1.Words.get(0).word+"_"+sent2.Words.get(0).word+" ");
					pw.print("LastS="+Arg1Last+"_"+Arg2Last+" ");
					
					String Arg1First3 = new String();
					if(sent1.Words.size() > 3)Arg1First3 = "Arg1First3=" + sent1.Words.get(0).word + "_" + sent1.Words.get(1).word + "_" + sent1.Words.get(2).word;
					else if(sent1.Words.size() > 2)Arg1First3 = "Arg1First3=" + sent1.Words.get(0).word + "_" + sent1.Words.get(1).word + "_NULL";
					else if(sent1.Words.size() > 1)Arg1First3 = "Arg1First3=" + sent1.Words.get(0).word + "_NULL_NULL";
					else Arg1First3 = "NULL_NULL_NULL";
					Arg1First3 += " ";
					pw.print(Arg1First3);
					
					String Arg1Last3 = new String();
					int n = sent1.Words.size();
					if(n > 3)Arg1Last3 = "Arg1Last3=" + sent1.Words.get(n-4).word + "_" + sent1.Words.get(n-3).word + "_" + sent1.Words.get(n-2).word;
					else if(n > 2)Arg1Last3 = "NULL_" + sent1.Words.get(n-3).word + "_" + sent1.Words.get(n-2).word;
					else if(n > 1)Arg1Last3 = "NULL_NULL_" + sent1.Words.get(n-2).word;
					else Arg1Last3 += "NULL_NULL_NULL";
					Arg1Last3 += " ";
					pw.print(Arg1Last3);
					
					String Arg2First3 = new String();
					if(sent2.Words.size() > 3)Arg2First3 = "Arg2First3=" + sent2.Words.get(0).word + "_" + sent2.Words.get(1).word + "_" + sent2.Words.get(2).word;
					else if(sent2.Words.size() > 2)Arg2First3 = "Arg2First3=" + sent2.Words.get(0).word + "_" + sent2.Words.get(1).word + "_NULL";
					else if(sent2.Words.size() > 1)Arg2First3 = "Arg2First3=" + sent2.Words.get(0).word + "_NULL_NULL";
					else Arg2First3 = "NULL_NULL_NULL";
					Arg2First3 += " ";
					pw.print(Arg2First3);
					
					pw.print(goldSense);
					
					pw.print("\n");
				}
				
				if(!doc.istrain){
					job = new JSONObject();
					Jarg = new JSONObject();
					arr = new JSONArray();
					for (int j=0; j<arg1.size();++j) {
						arr.add(arg1.get(j));
					}
					
					Jarg.put("TokenList", arr);
					job.put("Arg1", Jarg);
					
					Jarg = new JSONObject();
					arr = new JSONArray();
					for (int j=0; j<arg2.size();++j) {
						arr.add(arg2.get(j));
					}
					
					Jarg.put("TokenList", arr);
					job.put("Arg2", Jarg);
					arg1.clear();
					
					//addArg(arg1,connT);
					Jarg = new JSONObject();
					arr = new JSONArray();
					
					Jarg.put("TokenList", arr);
					job.put("Connective", Jarg);
					job.put("DocID", key);
					
					arr = new JSONArray();
					arr.add(goldSense);
					job.put("Sense", arr);
					job.put("Type", "Implicit");
					pw5.println(job.toJSONString());
					
				}
				
			}
		
		}
		pw1.close();
		pw2.close();
		pw.close();
		pw5.close();
		System.out.println(count);
		//System.out.println(count2);
		
		/*
		//nonExp: training and predicting
		System.out.println("training nonExp model");
		pw = new PrintWriter(new FileWriter(new File("output_nonExp.json")));
    	EventStream nonExp_train = new BasicEventStream(new PlainTextByLineDataStream(new FileReader(new File("nonExp_train.txt"))));
        AbstractModel nonExp_Model;
        nonExp_Model = GIS.trainModel(nonExp_train);
        writer = new SuffixSensitiveGISModelWriter(nonExp_Model, new File("nonExp_model.txt"));
        writer.persist();
		
        m = new GenericModelReader(new File("nonExp_model.txt")).getModel();
        pre = new Predict(m);
        ds = new PlainTextByLineDataStream(new FileReader(new File("nonExp_test.txt")));
        br = new BufferedReader(new FileReader( new File("nonExpTmp.json")));
        while (ds.hasNext()) {
        	s = (String) ds.nextToken();
        	String out = pre.eval(s.substring(0, s.lastIndexOf(' ')));
        	if(out.equals("Expansion"))out = "Expansion.Conjunction";
			if(out.equals("Comparison"))out = "Comparison.Contrast";
			if(out.equals("Temporal"))out = "Temporal.Synchrony";
			if(out.equals("alternative"))out = "Expansion.Alternative.Chosen alternative";
			if(out.equals("Temporal.Asynchronous"))out = "Temporal.Asynchronous.Precedence";
			
        	//String label = s.substring(s.lastIndexOf(' ')+1, s.length());
        	String oneLine = br.readLine();
        	jobj = (JSONObject)jparser.parse(oneLine);
        	JSONObject toWrite = new JSONObject();
          	toWrite.put("DocID", (String)jobj.get("DocID"));
          	toWrite.put("Arg1", jobj.get("Arg1"));
          	toWrite.put("Arg2", jobj.get("Arg2"));
          	toWrite.put("Connective", jobj.get("Connective"));
          	arr.clear();
          	arr.add(out);
          	toWrite.put("Sense", arr);
          	if(out.equals("EntRel"))
          		toWrite.put("Type", "EntRel");
          	else
          		toWrite.put("Type", "Implicit");
          	pw.println(toWrite.toJSONString());
        }
        pw.close();
        
        //combine two parts
        System.out.println("combining outputs");
        pw = new PrintWriter(new FileWriter(new File("output.json")));
        br = new BufferedReader(new FileReader( new File("output_Exp.json")));
        while(true){
        	s = br.readLine();
        	if(s == null)break;
        	pw.println(s);
        }
        br = new BufferedReader(new FileReader( new File("output_nonExp.json")));
        while(true){
        	s = br.readLine();
        	if(s == null)break;
        	pw.println(s);
        }
        pw.close();
        br.close();
        */
    }
	
	public static HashMap<String, String> connSense = new HashMap<String, String>();
	//store the sentences which had functioned as arg1, format: <docid, number list of sentences>
	public static HashMap<String, ArrayList<Integer>> preSentArg = new HashMap<String, ArrayList<Integer>>();
	//following 4 arrays refer to the nonExp part of gold labels
	public static ArrayList<String>nonExpGoldDoc = new ArrayList<String>();
	public static ArrayList<ArrayList<Integer>>nonExpGoldSpan1 = new ArrayList<ArrayList<Integer>>();
	public static ArrayList<ArrayList<Integer>>nonExpGoldSpan2 = new ArrayList<ArrayList<Integer>>();
	public static ArrayList<String>nonExpGoldSense = new ArrayList<String>();
	public static ArrayList<String>allSenseSet = new ArrayList<String>();
	public static ArrayList<String>nonExpSenseSet = new ArrayList<String>();
	public static HashMap<String, ArrayList<String>>paraEnd = new HashMap<String, ArrayList<String>>();
	
	private static void buildExpSense() throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader( new File("pdtb-data.json")));
		String s = "";
		
		while(true)
		{
			s = br.readLine();
			if(s == null)break;
			JSONParser jparser = new JSONParser();
			JSONObject jobj=(JSONObject) jparser.parse(s);
			JSONArray arr = (JSONArray)jobj.get("Sense");
			
			if(!jobj.get("Type").equals("Explicit"))continue;
			
			Integer id = Integer.parseInt(jobj.get("ID").toString());
			String sense = (String)(arr.get(0));
			expSense.put(id, sense);
		}
		br.close();
	}
	
	private static void buildParaEnd() throws IOException, ParseException {
		File folder = new File("raw");
		File[] files = folder.listFiles();
		for(File file:files){
			String fileName = file.getName();
			String pre = new String();
			if(!fileName.startsWith("wsj"))continue;
			
			BufferedReader br = new BufferedReader(new FileReader( new File("raw/"+fileName)));
			while(true){
				String s = br.readLine();
				if(s == null)break;
				if(s.trim().equals(".START"))continue;
				s = s.trim().replace(" ", "").toLowerCase().replaceAll("[\\pP\\p{Punct}]", "");
				
				if(s.length() == 0){
					if(pre.length() == 0)continue;
					if(paraEnd.get(fileName) == null){
						ArrayList<String>tmp = new ArrayList<String>();
						if(pre.length() >= 30)tmp.add(pre.substring(pre.length()-30));
						else tmp.add(pre);
						paraEnd.put(fileName, tmp);
					}
					else{
						if(pre.length() >= 30)paraEnd.get(fileName).add(pre.substring(pre.length()-30));
						else paraEnd.get(fileName).add(pre);
					}
				}
				pre = s;
			}
			br.close();
		}
		//for(int i = 0;i < paraEnd.get("wsj_0300").size();i++)
			//System.out.println(paraEnd.get("wsj_0300").get(i));
	}
	
	private static void buildAllSenseSet(){
		allSenseSet.clear();
		allSenseSet.add("EntRel");
		allSenseSet.add("Expansion.Conjunction");
		allSenseSet.add("Expansion.Restatement");
		allSenseSet.add("Contingency.Cause.Reason");
		allSenseSet.add("Comparison.Contrast");
		allSenseSet.add("Contingency.Cause.Result");
		allSenseSet.add("Expansion.Instantiation");
		allSenseSet.add("Temporal.Asynchronous.Precedence");
		allSenseSet.add("Comparison.Concession");
		allSenseSet.add("Temporal.Synchrony");
		allSenseSet.add("Temporal.Asynchronous.Succession");
		allSenseSet.add("Expansion.Alternative.Chosen alternative");
		
		allSenseSet.add("Contingency.Condition");
		allSenseSet.add("Expansion.Alternative");
		allSenseSet.add("Expansion.Exception");
	}
	
	private static void buildnonExpSenseSet(){
		nonExpSenseSet.clear();
		nonExpSenseSet.add("EntRel");
		nonExpSenseSet.add("Expansion.Conjunction");
		nonExpSenseSet.add("Expansion.Restatement");
		nonExpSenseSet.add("Contingency.Cause.Reason");
		nonExpSenseSet.add("Comparison.Contrast");
		nonExpSenseSet.add("Contingency.Cause.Result");
		nonExpSenseSet.add("Expansion.Instantiation");
		nonExpSenseSet.add("Temporal.Asynchronous.Precedence");
		nonExpSenseSet.add("Comparison.Concession");
		nonExpSenseSet.add("Temporal.Synchrony");
		nonExpSenseSet.add("Temporal.Asynchronous.Succession");
		nonExpSenseSet.add("Expansion.Alternative.Chosen alternative");
	}
	
	private static void buildNonExpGoldSpan() throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader( new File("pdtb-data.json")));
		String s = "";
		//PrintWriter pw = new PrintWriter(new FileWriter(new File("TEST.json")));
		while(true)
		{
			s = br.readLine();
			if(s == null)break;
			JSONParser jparser = new JSONParser();
			JSONObject jobj=(JSONObject) jparser.parse(s);
			JSONArray arr = (JSONArray)jobj.get("Sense");
			
			if(jobj.get("Type").equals("Explicit"))continue;
			//System.out.println((String)jobj.get("Type"));
			
			nonExpGoldDoc.add((String)jobj.get("DocID"));
			nonExpGoldSense.add((String)(arr.get(0)));

			ArrayList<Integer>tmp1 = new ArrayList<Integer>();
			//JSONObject doc = (JSONObject) jobj.get(key);
			JSONObject arg1 = (JSONObject) jobj.get("Arg1");
			JSONArray arg1List = (JSONArray) arg1.get("TokenList");
			//JSONArray arg1At0 = (JSONArray) arg1List.get(0);	
			for(int i = 0;i < arg1List.size();i++){
				JSONArray oneWord = (JSONArray) arg1List.get(i);
				String str = oneWord.get(2).toString();
				tmp1.add(Integer.valueOf(str));
			}
			nonExpGoldSpan1.add(tmp1);
			
			ArrayList<Integer>tmp2 = new ArrayList<Integer>();
			//JSONObject doc = (JSONObject) jobj.get(key);
			JSONObject arg2 = (JSONObject) jobj.get("Arg2");
			JSONArray arg2List = (JSONArray) arg2.get("TokenList");
			//JSONArray arg1At0 = (JSONArray) arg1List.get(0);	
			for(int i = 0;i < arg2List.size();i++){
				JSONArray oneWord = (JSONArray) arg2List.get(i);
				String str = oneWord.get(2).toString();
				tmp2.add(Integer.valueOf(str));
			}
			nonExpGoldSpan2.add(tmp2);
			/*
			JSONObject jjj = new JSONObject();
			jjj.put("doc", (String)jobj.get("DocID"));
			JSONArray aaa = new JSONArray();
			for(int i = 0;i < tmp1.size();i++)
				aaa.add(tmp1.get(i));
			jjj.put("arg1", aaa);
			
			JSONArray aa = new JSONArray();
			for(int i = 0;i < tmp2.size();i++)
				aa.add(tmp2.get(i));
			jjj.put("arg2", aa);
			
			jjj.put("sense", arr.get(0));
			
			pw.println(jjj);
			*/
		}
		br.close();
	}
	
	private static int getSentIDOfWord(Integer id, Integer docID){
		for(int i = 0;i < Doclist.size();i++){
			if(((Integer)Doclist.get(i).id).equals(docID)){
				Doc doc = Doclist.get(i);
				for(int j = 0;j < doc.Sentences.size();j++){
					Sentence sent = doc.Sentences.get(j);
					for(int k = 0;k < sent.Words.size();k++)
						if(((Integer)sent.Words.get(k).DocId).equals(id))
							return sent.id;
				}
			}
		}
		return -1;
	}
	
	private static void initSenseList() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader( new File("mfsmap.txt")));
		String s = null;
		while((s=br.readLine())!=null){
			String tmp[] = s.split("\t");
			connSense.put(tmp[0], tmp[2]);
		}
		br.close();
	}
	/*
	private static String getSense(String conn){
		String sense = connSense.get(conn);
		if(sense == null)return "Expansion.Conjunction";
		return sense;
		//System.out.println(sense);
		//return "Expansion.Conjunction";
	}
	*/
	private static void addArg(ArrayList<Integer> arg, TreeNode tn) {
		if (tn.isWord) {
			//do not add the last full stop to the argument
			if (tn.word.word.equals(".") && (tn.word.SentId == tn.word.Sent.Words.size()-1)) return;
			arg.add(tn.word.DocId);
			return;
		}
		for (int i=0; i<tn.children.size(); ++i) {
			addArg(arg,tn.children.get(i));
		}
	}
	
	public static ArrayList<TreeNode> connInTest = new ArrayList<TreeNode>();
	public static ArrayList<TreeNode> candiInTest = new ArrayList<TreeNode>();
	public static ArrayList<Integer> connST = new ArrayList<Integer>();
	public static ArrayList<Integer> connEN = new ArrayList<Integer>();
	public static ArrayList<String> connStr = new ArrayList<String>();
	
	private static String Check(int id, TreeNode tn) {
		if (tn.isWord) {
			if (tn.word.word.equals(".") && (tn.word.SentId == tn.word.Sent.Words.size()-1)) return "";
			for (int i=0; i<tn.word.links.size(); ++i) {
				if (tn.word.links.get(i).linknum == id) {
					return tn.word.links.get(i).linktype;
				}
			}
			return "null";
		}
		String ret = "";
		for (int i=0; i<tn.children.size(); ++i) {
			String tmp = Check(id, tn.children.get(i));
			if (ret == "") {
				ret = tmp;
			} else {
				if (tmp =="") continue;
				if (!ret.equals(tmp)) ret = "null";
			}
		}		
		return ret;
	}
	
	public static ArrayList<TreeNode> candidates = new ArrayList<TreeNode>();
	public static ArrayList<String> LeftRight = new ArrayList<String>();
	public static int stID=0;
	public static int enID=0;
	
	private static TreeNode findNode(Word curWord, String[] conn) {
		if (conn.length==1) return curWord.node;
		TreeNode ret = curWord.node;
		while (true) {
			ret = ret.father;
			if (ret==null) {
				System.out.println("Error: Bad Tree!!");
				break;
			}
			connIndex = 0;
			stNode = null;
			enNode = null;
			Contain(ret,conn);
			if (connIndex == conn.length) {
	    		stID = stNode.word.DocId;
	    		enID = enNode.word.DocId;
				while (!ret.equals(stNode)) {
					int ind = stNode.childIndex;
					stNode = stNode.father;
					for (int i=0; i<ind; ++i) {
						candidates.add(stNode.children.get(i));
						LeftRight.add("left");
					}
				}
				while (!ret.equals(enNode)) {
					int ind = enNode.childIndex;
					enNode = enNode.father;
					for (int i=ind+1; i<enNode.children.size(); ++i) {
						candidates.add(enNode.children.get(i));
						LeftRight.add("right");
					}
				}
				break;
			}
		}
		return ret;
	}
	
	public static int connIndex = 0;
	public static TreeNode stNode,enNode = null;
	
	private static void Contain(TreeNode tn, String[] s) {
		if (tn.isWord && tn.content.toLowerCase().equals(s[connIndex]) && tn.word.isConn) {
            //start and end node of the conn
			if (connIndex == 0) stNode = tn;
			if (connIndex == s.length-1) enNode = tn;
			connIndex++;
			return;
		}
		for (int i=0; i<tn.children.size();++i) {
			Contain(tn.children.get(i),s);
			if (connIndex == s.length) return;
		}
	}
	
	
}
