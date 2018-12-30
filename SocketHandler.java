import java.nio.ByteBuffer;

class SocketHandler
{
	private enum ParsingState { METHOD_STATE, STAT_TYPE_STATE, PARAM_STATE, FINISH_STATE };
	private static final String FINISH_INDICATOR=" HTTP";
	
	private StatsKeeper statsKeeper;
	private StringBuilder parsingData;
	private ByteBuffer responseData;
	private TrieNode currentNode;
	private String statType,result;
	private ParsingState state;
	
	public SocketHandler(StatsKeeper statsKeeper)
	{
		this.statsKeeper=statsKeeper;
		parsingData=new StringBuilder();
		state=ParsingState.METHOD_STATE;
	}
	
	public void finish() 
	{
		state=ParsingState.FINISH_STATE;
		if (result==null) result="";
	}
	
	public boolean finished() { return (state==ParsingState.FINISH_STATE); }
	public String getResult() { return result; }
	
	public ByteBuffer getResponseData() { return responseData; }
	public void setResponseData(ByteBuffer responseData)
	{ this.responseData=responseData; }
	
	public void handleMoreData(String requestData)
	{
		if ((result!=null)&&(result.equals(""))) return;
		parsingData.append(requestData);
		int indicatorIndex=parsingData.indexOf(FINISH_INDICATOR,parsingData.length()-
				requestData.length()-FINISH_INDICATOR.length());
		if (indicatorIndex>-1)
			parsingData.delete(indicatorIndex,parsingData.length());
		
		boolean done=false; int endIndex=0;
		while (!done)
		{
			switch (state)
			{
				case METHOD_STATE:
					endIndex=5;
					if (parsingData.length()>=endIndex)
					{
						if (parsingData.substring(0,endIndex).equals("GET /"))
							state=ParsingState.STAT_TYPE_STATE;
						else result="";
					}
					else { endIndex=0; done=true; }
					break;
				case STAT_TYPE_STATE:
					int separatorIndex=parsingData.indexOf("/",endIndex);
					if (separatorIndex>-1)
					{
						statType=parsingData.substring(endIndex,separatorIndex);
						if ((!statType.equals("event"))&&(!statType.equals("word")))
							result="";
						else 
						{
							state=ParsingState.PARAM_STATE; 
							endIndex=separatorIndex+1;
						}
					}
					else if (parsingData.length()-endIndex>=Math.max("event".length(),"word".length()))
						result="";
					else done=true;
					break;
				case PARAM_STATE:
					String statParam=parsingData.substring(endIndex);
					if (statParam.length()>0)
					{
						if (statType.equals("event"))
							currentNode=statsKeeper.getEventFrequencyNode(statParam,currentNode);
						else if (statType.equals("word"))
							currentNode=statsKeeper.getWordFrequencyNode(statParam,currentNode);
						result="Count: " + (currentNode!=null?currentNode.count:0);
					}
					else done=true;
					break;
			} //end switch
			if (result!=null)
			{
				done=true;
				if (result.equals("")) 
					state=ParsingState.FINISH_STATE;
			}
		} //end while
		
		if ((result==null)||(!result.equals("")))
			parsingData.delete(0,endIndex);
		if (indicatorIndex>-1)
		{
			state=ParsingState.FINISH_STATE;
			if (result==null) result="";
		}
	}
}
