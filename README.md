====================================================================================================
====================================================================================================

README File for Project - Iteration 2 - TFTP 

	Derek Dore
	Lee Fisher
	Lexi Brown
	Scott Hanton
	
====================================================================================================
====================================================================================================

USING THE TFTP 
======================
OVERVIEW
=========
	This is a tftp client/server that is being used to transfer file from the client to the server. 
	It also displays information about the transfer process. The client is setup on Port 69 in the 
	Normal run and Port 23 otherwise, it either perform a read request or write request 
	The transfer process starts with a request to send or receive file and when this request is granted, 
	a file of block length of 512 bytes is sent, each data packet is first acknowledge 
	by an acknowledgement packet before the next packet is sent.

====================================================================================================
====================================================================================================

RESPONSIBILITIES
================

Richard Hanton
- Created the timing diagrams for error codes 1, 2, 3, and 6. 
- Created the UML diagram for the state of our project at iteration 2.
- Helped team members fix some small issues within client and server.

Derek Dore


Lee  Fisher


Lexi Brown
- Updating readme

====================================================================================================
====================================================================================================

RUNNING THE PROGRAM
=====================

	Client Side:
		1. Import project into Eclipse.
		2. Run the ClientInterface

	Server Side:
		1. Import project into Eclipse.
		2. Run the Server

RUNNING THE PROGRAM IN TEST OR VERBOSE MODE
===========================================

	Client:
		While ClientInterface is running you can type in the following commands:
			* set verbose on
			* set verbose off
			* set mode normal
			* set mode test
	Server:
		While the server is running you can type in the following commands:
			* set verbose on
			* set verbose off

IMPLEMENTATION DETAILS
======================

1. Client.java implements the client side of the TFTP as follows:

	* Create a read request or write request base on the request type.
	* Creates a socket, generate a random port number and bind it to the socket.
	* Connect to the server through the port number and IP Address of the server.
	* Generate array byte and send to server for a request type and file.
	* Create Datagram packet and send the message, message length, IP Address and port number to server.
	* If it is Read request, save incoming file packet to path and print it to the terminal.
	* Check the length of datagram to check if it was the last packet.
	* Check the name of the file and send file to the server if it doesnt already exist.
	* Send Acknowledgement to the server when the block is received

2. ClientInterface.java implements the client side of the TFTP as follows: 
	* Create a Client class
	* Print operation information to the terminal
	* Convert the buffer readline to integer and print it to the terminal
	* Send File based on the integer value of the buffer reader.

3. Server.java implements the server side of the TFTP as follows:
	* Create a datagram socket and binds it to port 69 
	* Starts listening on the socket
	* Wait for client to connect and create thread to handle it.
	* Accept datagram packet from the socket and process the received datagram.
	* Start the server thread 
	* Listen for connections and start another to wait for console input to shutdown server
	* Close all the threads when they are finished.

4. ClientConnection.java 
	* Handle request and Create response
	* Create new server socket and send packet
	* wait to receive datagram packet, block until a datagram packet is received.
	* Process the received datagram packet.
	* Package file to be read, write to specified file and create new file if file doesnt exist.
	* Verify that the first 4 bytes are ACK and hat the request is valid.
	* Return the name of the file.

5. ErrorSimulator.java
	* Create a datagram socket and binds it to port 23 
	* Starts listening on the socket
	* Wait for client to connect and creates thread to handle it.
	* Accept datagram packet from the socket and pass the received datagram to the server.
	* Wait for server to send back response and then pass response to client
	* Continues to pass data between client and server until file transfer is complete.

OTHER FILES
===========

1. Logger.java
	* This is a class that logs all packets coming in and going out
	* It logs the contents of the packet, it's length, type (i.e, RRQ, WRQ, ACK, etc), block number, mode, etc

2. Variables.java
	* Holds all the common variables used by the client and sever

3. test.txt
	* A short file for the client to test writing

4. text.txt
	* A long file for the client to test writing large files

5. test2.txt
	* A short file for the client to test reading

4. text.txt
	* A long file for the client to test reading large files

5. UML.png
	* The UML (Unified Modeling Language) for each class

6. Error Code *.jpeg
	* All the timing diagrams from errors while reading or writing

TERMINATION
============
* A data packet size less than 512 byte is sent to mark the end of a transfer.

* If some errors occur due to an invalid request then instead of sending an error 
packet and corrupting the program, a stack trace is printed.

LIMITATIONS
============
* This program is limited to reading and writing files from/to a remote server. 

* It passes 8 bit byte of data.

ASSUMPTIONS
============
* No Packets are duplicated in transit.

* No Pakets will be delayed or lost in transit.

FEATURES
========
* Writes File to a directory and doesn't override file if the already exists.

* Perform a read request or write request base on the user input.
