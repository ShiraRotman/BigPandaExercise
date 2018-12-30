
class TrieNode
{
	char letter; int count;
	TrieNode firstChild,nextSibling;
}

class StatsKeeper
{
	private TrieNode eventsRootNode=new TrieNode();
	private TrieNode wordsRootNode=new TrieNode();
	
	private class SearchResult 
	{ int lastIndex; TrieNode lastNode; }
	
	public StatsKeeper() { }
	
	public TrieNode getEventFrequencyNode(String eventType,TrieNode startNode)
	{
		if (startNode==null) startNode=eventsRootNode;
		SearchResult result=findExpression(eventType,startNode);
		return (result.lastIndex==eventType.length()-1?result.lastNode:null);
	}
	
	public TrieNode getWordFrequencyNode(String dataWord,TrieNode startNode)
	{
		if (startNode==null) startNode=wordsRootNode;
		SearchResult result=findExpression(dataWord,startNode);
		return (result.lastIndex==dataWord.length()-1?result.lastNode:null);
	}
	
	public void incrementEventFrequency(String eventType)
	{ incrementFrequency(eventType,eventsRootNode); }
	
	public void incrementWordFrequency(String dataWord)
	{ incrementFrequency(dataWord,wordsRootNode); }
	
	private void incrementFrequency(String expression,TrieNode rootNode)
	{
		SearchResult result=findExpression(expression,rootNode);
		TrieNode matchNode=null;
		if (result.lastIndex==expression.length()-1)
			matchNode=result.lastNode;
		else
		{
			int index=result.lastIndex+1;
			TrieNode connectNode=result.lastNode.firstChild;
			if (connectNode!=null)
			{
				while (connectNode.nextSibling!=null)
					connectNode=connectNode.nextSibling;
				matchNode=new TrieNode();
				matchNode.letter=expression.charAt(index++);
				connectNode.nextSibling=matchNode;
				connectNode=matchNode;
			}
			else connectNode=result.lastNode;
			for (;index<expression.length();index++)
			{
				matchNode=new TrieNode();
				matchNode.letter=expression.charAt(index);
				connectNode.firstChild=matchNode;
				connectNode=matchNode;
			}
		}
		matchNode.count++;
	}
	
	private SearchResult findExpression(String expression,TrieNode rootNode)
	{
		if (expression==null)
			throw new NullPointerException("The expression to search for must be non-null!");
		SearchResult result=new SearchResult();
		TrieNode currentNode=rootNode; 
		int index=0; boolean finished=false;
		while ((!finished)&&(index<expression.length()))
		{
			TrieNode childNode=currentNode.firstChild;
			while ((childNode!=null)&&(childNode.letter!=expression.charAt(index)))
				childNode=childNode.nextSibling;
			if (childNode==null) finished=true;
			else
			{
				currentNode=childNode;
				index++;
			}
		}
		result.lastIndex=index-1;
		result.lastNode=currentNode;
		return result;
	}
}