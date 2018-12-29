import java.nio.ByteBuffer;

class SocketHandler
{
	private ByteBuffer responseData;
	private boolean finished;
	
	public SocketHandler() { }
	
	public void handleMoreData(String data) { finished=true; }
	
	public boolean finished() { return finished; }
	public void finish() { finished=true; }
	
	public ByteBuffer getResponseData() { return responseData; }
	public void setResponseData(ByteBuffer responseData)
	{ this.responseData=responseData; }
}
