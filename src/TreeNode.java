import java.util.ArrayList;


public class TreeNode {
	int childIndex;
	Sentence Sent;
	boolean isWord = false;
	String content = "";
	TreeNode father = null;
	Word word = null;
	ArrayList<TreeNode> children = new ArrayList<TreeNode>();
	
	TreeNode(boolean b) {
		isWord = b;
	}
}
