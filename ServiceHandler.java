import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.regex.*;

public class ServiceHandler
{
	public static final int MAX_VALID_PORT=65535;
	private static final int BUFFER_SIZE=1024;
	
	public static void main(String[] args)
	{
		int port=0;
		if (args.length>=1)
		{
			try { port=Integer.parseInt(args[0]); }
			catch (NumberFormatException nfe)
			{
				System.out.println("Non-numeric port number supplied!");
				System.exit(-1);
			}
			if ((port<0)||(port>MAX_VALID_PORT))
			{
				System.out.println("The port number supplied is not in the allowed range of 0 and " + MAX_VALID_PORT + "!");
				System.exit(-1);
			}
		}
		
		Process generator=null;
		Selector selector=null; InputStreamReader generatorStream=null;
		try (ServerSocketChannel serverChannel=ServerSocketChannel.open())
		{
			serverChannel.bind(new InetSocketAddress("127.0.0.1",port));
			serverChannel.configureBlocking(false);
			selector=Selector.open();
			serverChannel.register(selector,SelectionKey.OP_ACCEPT);
			Charset converter=Charset.forName("US-ASCII");
			ByteBuffer requestBuffer=ByteBuffer.allocate(BUFFER_SIZE);
			System.out.println("Listening on port: " + serverChannel.socket().getLocalPort());
			
			ProcessBuilder processBuilder=new ProcessBuilder("generator-windows-amd64.exe");
			try { generator=processBuilder.start(); }
			catch (IOException ioe)
			{ 
				System.out.println("Could not execute the generator process!");
				System.out.println(ioe.getMessage());
			}
			StringBuilder generatorData=null; char[] genBuffer=null;
			if (generator!=null)
			{
				generatorStream=new InputStreamReader(generator.getInputStream(),converter);
				generatorData=new StringBuilder(100); genBuffer=new char[BUFFER_SIZE];
			}	
			Pattern genLinePattern=null;
			StatsKeeper statsKeeper=new StatsKeeper();
			
			while (true)
			{
				if (generator!=null)
				{
					boolean isReady=false;
					try { isReady=generatorStream.ready(); }
					catch (IOException ioe1)
					{
						try { generatorStream.close(); }
						catch (IOException ioe2) { }
						generator.destroy();
					}
					
					if (isReady)
					{
						int numRead=0;
						try { numRead=generatorStream.read(genBuffer); }
						catch (IOException ioe1)
						{
							try { generatorStream.close(); }
							catch (IOException ioe2) { }
							generator.destroy();
						}
						if (numRead==-1)
						{
							try { generatorStream.close(); }
							catch (IOException ioe) { }
							generator.destroy();
						}
						else if (numRead>0)
						{
							generatorData.append(genBuffer,0,numRead);
							if (genLinePattern==null)
							{
								genLinePattern=Pattern.compile("\\Q{ \"event_type\": \"\\E(?<event>\\w+)\\Q\", " + 
										"\"data\": \"\\E(?<word>\\w+)\\Q\", \"timestamp\": \\E\\d+ \\}");
							}
							Matcher matcher=genLinePattern.matcher(generatorData); int endIndex=0;
							while (matcher.find())
							{
								statsKeeper.incrementEventFrequency(matcher.group("event"));
								statsKeeper.incrementWordFrequency(matcher.group("word"));
								endIndex=matcher.end();								
							}
							generatorData.delete(0,endIndex);
						}
					} //end if isReady
				} //end if generator...
				
				int numSelected=0;
				try { numSelected=selector.selectNow(); }
				catch (IOException ioe) 
				{ handleCriticalError("Could not poll for ready channels!",ioe); }
				if (numSelected>0)
				{
					for (SelectionKey channelKey : selector.selectedKeys())
					{
						if (channelKey.isAcceptable())
						{
							SocketChannel socketChannel=null;
							try 
							{ 
								socketChannel=serverChannel.accept();
								if (socketChannel!=null)
								{
									socketChannel.configureBlocking(false);
									SocketHandler socketHandler=new SocketHandler(statsKeeper);
									socketChannel.register(selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE,socketHandler);
								}
							}
							catch (IOException ioe) { }
						}
						else
						{
							SocketChannel socketChannel=(SocketChannel)(channelKey.channel());
							SocketHandler socketHandler=(SocketHandler)(channelKey.attachment());
							if ((channelKey.isReadable())&&(!socketHandler.finished()))
							{
								requestBuffer.clear(); int numRead=0;
								try { numRead=socketChannel.read(requestBuffer); }
								catch (IOException ioe1) 
								{
									channelKey.cancel();
									try { socketChannel.close(); }
									catch (IOException ioe2) { }
								}
								if (numRead==-1) socketHandler.finish();
								else if (numRead>0)
								{
									requestBuffer.flip();
									String requestData=converter.decode(requestBuffer).toString();
									socketHandler.handleMoreData(requestData);
								}
							}
							
							if ((channelKey.isValid())&&(channelKey.isWritable())&&(socketHandler.finished()))
							{
								ByteBuffer responseData=socketHandler.getResponseData();
								if (responseData==null)
								{
									try { socketChannel.shutdownInput(); }
									catch (IOException ioe) { }
									String lineSeparator="\r\n";
									StringBuilder responseBuilder=new StringBuilder(100);
									String content=socketHandler.getResult();
									if (content.equals(""))
									{
										responseBuilder.append("400 Bad Request");
										responseBuilder.append(lineSeparator);
										responseBuilder.append("Connection: close");
										responseBuilder.append(lineSeparator);
									}
									else //Succeeded
									{
										responseBuilder.append("200 OK");
										responseBuilder.append(lineSeparator);
										responseBuilder.append("Connection: close");
										responseBuilder.append(lineSeparator);
										responseBuilder.append("Content-Type: text/plain");
										responseBuilder.append(lineSeparator);
										responseBuilder.append("Content-Length: ");
										responseBuilder.append(content.length());
										responseBuilder.append(lineSeparator);
										responseBuilder.append(lineSeparator);
										responseBuilder.append(content);
									}
									responseData=converter.encode(responseBuilder.toString()).asReadOnlyBuffer();
									socketHandler.setResponseData(responseData);
								}
								
								try { socketChannel.write(responseData); }
								catch (IOException ioe1)
								{
									channelKey.cancel();
									try { socketChannel.close(); }
									catch (IOException ioe2) { }
								}
								if (!responseData.hasRemaining())
								{
									channelKey.cancel();
									try { socketChannel.close(); }
									catch (IOException ioe) { }
								}
							} //end if (isValid...)
						} //end else (if isAcceptable...)
					} //end for
				} //end if numSelected...
				selector.selectedKeys().clear();
			} //end while
		} //end try
		catch (IOException ioe)
		{ handleCriticalError("Could not register the service!",ioe); }
		finally
		{
			if (generator!=null)
			{
				try { generatorStream.close(); }
				catch (IOException ioe) { }
				generator.destroy();
			}
			try { selector.close(); }
			catch (IOException ioe) { }
		}
	}
	
	private static void handleCriticalError(String message,Exception exception)
	{
		System.out.println(message);
		System.out.println(exception.getMessage());
		System.exit(-1);
	}
}