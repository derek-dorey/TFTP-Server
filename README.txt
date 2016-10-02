====================================================================================================
====================================================================================================

README File for Project - Iteration 0 and 1 - TFTP 

	Names and Student Number:
	Lexi Brown
	Derek Dore
	Adebola Shittu   100918348
	Scott  Hanton
	Lee  Fisher
	
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


RUNNING THE PROGRAM
=====================
   
1. Copy all the files in the same directory

2. Using command prompt:

    Setting Up the Compiler

      A.Copying the address of java jdk compiler
	   * My computer --> Local disk --> Program files --> Java --> jdk --> bin < copy location from properties >
	
      B.Copy and paste compiler address to the computer
	   * My computer -- >  properties --> advance system settings --> System properties --> Environment variables
 			 -- > Create New user variable --> Variable Name: path --> Paste the location copied from Step A
	
   
    * start menu --->  command prompt(cmd)

	* C:\Users\User-name>javac  press ENTER
	* C:\Users\User-name>cd\                                (Go to the beginning of your hard drive)
	* C:\dir                                                (print out all the directory in your c drive)
	* C:\>cd <directory of the file(filename)>              (open the directory of your file where the project is saved)
	* C:\filename>javac server.java				
	* C:\filename>javac Client.java
	* C:\filename>javac ClientInterface.java
	* C:\filename>javac ClientConnection.java


3. Create New project using Eclipse or bluej. Create class for the client, server, clientInterface, ClientConnection.
   Build the program and compile it.
       

DISPLAY ON TERMINAL
=====================
	~$ cd <path-to-main-directory>
	
	Client:
	~$ cd <path-to-client-directory>
	~$ ./client <IP address of server> <port number>

	Server:
	~$ cd <path-to-server-directory>
	~$ ./server <port number>
	


TERMINATION
============
*A data packet size of 0 byte is sent to mark the end of a transfer. The data packet is 
first acknowledge by the ACK packets like all other data packets.

*If some errors occur due to an invalid request then instead of sending an error 
packet and corrupting the program, a stack trace is printed.


LIMITATIONS
============
* This program is limited to reading and writing files from/to a remote server. 

*It passes 8 bit byte of data.

*Not programmed to deal with when "File is not found" or when "the disk is full".

*Can not overwrite file if it already exist.

ASSUMPTIONS
============
* No Packets are duplicated in transit.

* No Pakets will be delayed or lost in transit.

* No TFTP error packet is prepared, transmitted, received or handled.

FEATURES
========
* Writes File to a directory but doesnt over ride file if the already exists.

* Perform a read request or write request base on the user input.


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
	* Print operatin information to the terminal
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
