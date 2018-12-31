How to use the stats service
----------------------------
After pulling the repository, compile the code by the command:
javac ServiceHandler.java

Then you can run the service by using:
java ServiceHandler [port-number]

If you don't specify a port number, it will be allocated by the OS.
(You will see a message with the port the service is listening to)
Then you can use a browser or any other HTTP client to connect to the service and ask for stats.

To get the count for an event type:
localhost:{port}/event/{event-type}

To get the count for a data word:
localhost:{port}/word/{word}

Possible improvements:
----------------------
1. Right now the service only supports getting stats for specific events and words,
meaning the user has to know what to ask for. Adding an option for getting a list of 
all events / words that were found in the input source would let the user see what's 
available.

2. Each request made to the service gets a new SocketHandler object that parses it 
and generates the response body. This object can only handle one request and gets 
destroyed once the communication with the client is complete. A better way would be 
to add a reset function to the class, and using a pool to manage and allocate several 
instances to clients. That way objects won't get created and garbage collected all 
the time, which will also help prevent DOS attacks. However, it might cause blocking 
if there are no free instances, depending on the pool's policy.

3. The stats keeper could use more encapsulation to prevent other classes from being 
able to directly manipulate the data structures used to store the stats. Also, the 
managing of the tries could be optimized by storing several characters in each node, 
and only keeping track of the actual strings that were inserted.
