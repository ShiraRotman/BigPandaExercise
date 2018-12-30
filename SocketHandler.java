import java.nio.ByteBuffer;

class SocketHandler
{
	private enum ParsingState { METHOD_STATE, STAT_TYPE_STATE, PARAM_STATE };
	
	private StringBuilder parsingData;
	private ByteBuffer responseData;
	private String statType,result;
	private ParsingState state;
	
	public SocketHandler()
	{ parsingData=new StringBuilder(); state=ParsingState.METHOD_STATE; }
	
	public boolean finished() { return (result!=null); }
	public void finish() { if (result==null) result=""; }
	public String getResult() { return result; }
	
	public ByteBuffer getResponseData() { return responseData; }
	public void setResponseData(ByteBuffer responseData)
	{ this.responseData=responseData; }
	
	public void handleMoreData(String requestData)
	{
		if ((result!=null)&&(result.equals(""))) return;
		parsingData.append(requestData);
		String lineSeparator="\r\n";
		int lineSepIndex=parsingData.indexOf(lineSeparator,parsingData.length()-
				requestData.length()-lineSeparator.length());
		if (lineSepIndex>-1) 
			parsingData.delete(lineSepIndex,parsingData.length());
		
		boolean finished=false; int endIndex=0;
		while (!finished)
		{
			System.out.println("End: " + endIndex + " State: " + state);
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
					else { endIndex=0; finished=true; }
					break;
				case STAT_TYPE_STATE:
					int separatorIndex=parsingData.indexOf("/",endIndex);
					if (separatorIndex>-1)
					{
						statType=parsingData.substring(endIndex,separatorIndex);
						System.out.println("Stat: " + statType);
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
					else finished=true;
					break;
				case PARAM_STATE:
					String statParam=parsingData.substring(endIndex);
					result=(statParam.length()>0?"Count: 0":"");
					//TODO: Search and update pointer and endIndex
					break;
			}
			if (result!=null) finished=true;
		}
		
		if ((result==null)||(!result.equals("")))
			parsingData.delete(0,endIndex);
		if ((result==null)&&(lineSepIndex>-1)) result="";
	}
}
