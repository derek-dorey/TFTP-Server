package com.sysc.tftp.client;
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.
 
import java.io.*;
import java.net.*;
import com.sysc.tftp.utils.Variables;

public class Client {

   private DatagramPacket sendPacket, receivePacket;	
   private DatagramSocket sendReceiveSocket;
   private String transferMode = "octet";
   
   private static final int DATA_PACKET_HEADER_SIZE = 4;//Size of the header section of a data packet (op code + block #)
   private static final int ACK_DATA_SIZE = 4;			//Size of the ack packet 
   
   private String fileFolder = System.getProperty("user.dir") + "/files/";	//Path to folder where files will be stored
  

   
   public Client() {
   }

   /**
    * Create either a new read request or a new write request
    * which will be sent to the server before a file transfer
    * takes place.
    */
   private byte[] newRequest(byte rqType, String fileName) {
      byte[] msg = new byte[Variables.MAX_PACKET_SIZE], // Message we send
              fn, 										// Filename as an array of bytes
              md; 										// Mode as an array of bytes

      int len;        									// Length of the message
      
      //Set request type (RRQ = 1 or WRQ = 2)
      msg[0] = 0;
      msg[1] = rqType;
      
      //Get filename as bytes
      fn = fileName.getBytes();
      
      //Copy filename into message
      System.arraycopy(fn,0,msg,2,fn.length); 

      //Add 0 byte
      msg[fn.length+2] = 0;
      
      //Get mode as bytes
      md = this.transferMode.getBytes();
    		  
      //Add mode bytes to message
      System.arraycopy(md,0,msg,fn.length+3,md.length);
      
      //Get length of message
      len = fn.length+md.length+4; // length of the message		
      
      //Finish message with another 0 byte
      msg[len-1] = 0;
      
      //Return message request as bytes
      return msg;
      
   }
   
   /**
    * Receive a file from the server based on the supplied
    * filename.
    */
   private void receiveFile(String fileName) {
	   byte[] requestMsg;			//Message containing request being sent to the server
       int sendPort;				//Port we are sending the packet to
       int tid;						//Random transfer ID generated 
       Variables.Mode run = Variables.Mode.NORMAL; 		//Normal sends to server directly, TEST sends to simulator
	   DatagramPacket sendPacket;	//A packet to send request to server

	   //Start of Try/Catch
	   try { 

		   //Generate random tid for client
		   tid = 1 + (int)(Math.random() * ((10000) + 1));
	
		   //Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);
		   
		   //If normal run mode use port 69, port 23 otherwise
		   if (run == Variables.Mode.NORMAL) 
			   sendPort = Variables.NORMAL_PORT;
		   else
			   sendPort = Variables.TEST_PORT;
		      
	       //Generate byte array to send to server for request type and file
	       requestMsg = newRequest((byte) 1, fileName);
	    	
	       //Start of Try/Catch
	       try {
	    	   
	    	   //Create new datagram packet to send
	    	   sendPacket = new DatagramPacket(requestMsg, requestMsg.length,
	                           InetAddress.getLocalHost(), sendPort);
	    
	    	   //Send the packet to the server
	    	   sendReceiveSocket.send(sendPacket);   
	    	     
	    	   //Save the file to the client machine
	    	   saveFileData(fileFolder + fileName);
	    	   
	       //End of Try/Catch
	       } catch (UnknownHostException e) {
	    	   e.printStackTrace();
	    	   System.exit(1);
	    
	       //End of Try/Catch
	       } catch (IOException e) {
	    	   e.printStackTrace();
	    	   System.exit(1);	    	   
	       }
		  
	    //End of Try/Catch
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	   
   }
   
   /**
    * Saves incoming file packets to the specified path.
    * Also sends an ack back to the server for each packet
    * received.
    */
   private void saveFileData(String filePath) {
	   File f = new File(filePath);			//File object for seeing if it already exists
	   FileOutputStream incoming;			//FileOutputStream for incoming data
	   int currentBlock = 0;				//Current block of data being received
	   int currentBlockFromPacket = 0;		//Current block # from the packet
	   DatagramPacket receivePacket;		//Incoming datagram packet
	   byte[] data = new byte[Variables.MAX_PACKET_SIZE];			//Byte array for packet data
	   
	   //Start of Try/Catch
       try {
    	   
    	   //Check if file already exists
    	   if(f.exists() && !f.isDirectory()) { 
    	       
    		   //File already exists
    		   System.out.println("The file already exists on the client, we will not overwrite it.");
    		   return;
    		   
    	   }
    	   
    	   //Open new FileOutputStream to place file
    	   incoming = new FileOutputStream(filePath);	   
    	 
    	   //Initialize receivePacket
    	   receivePacket = new DatagramPacket(data, data.length);
    	   
    	   //While we have more packets to receive, loop
    	   do {
    		   
    		   //Increment block counter
    		   currentBlock ++;
    		   
    		   //Receive an incoming packet
    		   sendReceiveSocket.receive(receivePacket);
    		   
    		   //Start of Try/Catch
    		   try {
    			   
    			   //Check if data packet
    			   if (data[0] == 0 && data[1] == 3) {
    				 
    				   //Extract block # from incoming packet
    				   int blockNumber = ((data[2] << 8) & 0xFF00) & (data[3] & 0xFF);
    						   
	  	    		   //Write packet data to the file
		    		   incoming.write(receivePacket.getData());
		    		   
		    		   //Wait for data to be written to file before doing anything else
		    		   incoming.getFD().sync();
	    		   
		    		   //Send ACK for received block back to server
		    		   sendACK(currentBlock, receivePacket.getPort());
	    		   
    			   } else {
    				   
    				   //Invalid op code
    				  
    			   }
    			   
	    	   //Sync failed, we could not save the bytes
    		   } catch(SyncFailedException e) {
    			   
    			   //Output error and return
    			   System.out.println("Could not save the full file! An error occured");
    			   return;
    			   
    		   }
    		   
    	   } while (!lastBlock(receivePacket));
    	   
    	   //Close FileOutputStream
    	   incoming.close();
    	   
       //End of Try/Catch
       } catch (IOException e) {
    	   e.printStackTrace();
    	   System.exit(1);
       }
   }
   
   /**
    * Checks the length of a datagram packet. If it is less
    * than 512 bytes it is the last packet for the file.
    */
   public boolean lastBlock(DatagramPacket datagramPacket) {
	   
	   //If datagram packet length is less than MAX_PACKET_SIZE its the last block
	   if (datagramPacket.getLength() < Variables.MAX_PACKET_SIZE) {
		   return true;
	   } else {
		   return false;
	   }
   }
   
   /**
    * Send a file to the server based on the supplied
    * filename.
    */   
   public void sendFile(String fileName) {
	   byte[] requestMsg;							//Message containing request being sent to the server
       int sendPort;								//Port we are sending the packet to
       int tid;										//Random transfer ID generated 
       Variables.Mode run = Variables.Mode.NORMAL; 						//Normal sends to server directly, TEST sends to simulator
	   DatagramPacket sendPacket;					//A packet to send request to server
	   byte[] ackData = new byte[ACK_DATA_SIZE];	//Byte array for packet data
	   
	   //Start of Try/Catch
	   try { 

		   //Generate random tid for client
		   tid = 1 + (int)(Math.random() * ((10000) + 1));
	
		   //Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);
		   
		   //If normal run mode use port 69, port 23 otherwise
		   if (run == Variables.Mode.NORMAL) 
			   sendPort = Variables.NORMAL_PORT;
		   else
			   sendPort = Variables.TEST_PORT;
		     
		   //Start of Try/Catch
		   try {
			   
		       //Generate byte array to send to server for request type and file
		       requestMsg = newRequest((byte) 2, fileName);
		       
	    	   //Create new datagram packet to send
	    	   sendPacket = new DatagramPacket(requestMsg, requestMsg.length,
	                           InetAddress.getLocalHost(), sendPort);
	    
	    	   //Send the packet to the server
	    	   sendReceiveSocket.send(sendPacket);   
	    	   
	    	   //Print the packet
	    	   System.out.println("Client: sending data packet");
	    	   System.out.println("To host: " + sendPacket.getAddress());
	    	   System.out.println("Destination host port: " + sendPacket.getPort());
	    	   System.out.println("Length: " + requestMsg.length);
	    	   System.out.println("Containing: ");
	    	   for (int j = 0; j < requestMsg.length; j++) {
	    		   System.out.println("byte " + j + " " + requestMsg[j]);
	    	   }
	    	   
				// Form a String from the byte array, and print the string.
				String sending = new String(requestMsg, 0, requestMsg.length);
				System.out.println(sending);
				
	    	   //Send the file to the server 
	    	   sendFileData(fileName);
    	   
	       //End of Try/Catch
	       } catch (UnknownHostException e) {
	    	   e.printStackTrace();
	    	   System.exit(1);
	    
	       //End of Try/Catch
	       } catch (IOException e) {
	    	   e.printStackTrace();
	    	   System.exit(1);	    	   
	       }
	       
	    //End of Try/Catch
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}       	     
	    		   
   }
   
   /**
    * Sends outgoing file packets containing the file
    * data to the server.
    */
   private void sendFileData(String filePath) {
	   File f = new File(filePath);					//File object for seeing if it already exists
	   FileInputStream outgoing;					//FileOutputStream for outgoing data
	   byte[] ackData = new byte[ACK_DATA_SIZE];	//Byte array for ack packet data
	   byte[] data = new byte[Variables.MAX_PACKET_SIZE];		//Byte array for file data being sent
	   byte[] dataSection = new byte[Variables.MAX_PACKET_SIZE - DATA_PACKET_HEADER_SIZE];//Size of the data in the packet
	   int blockNumber = 0;							//Current block number being sent
	   int bytesRead = 0;							//Number of bytes read
	   
	   //Check if file already exists
	   if(!f.exists() || f.isDirectory()) { 
	       
		   //File already exists
		   System.out.println("The file you are trying to send does not exist");
		   return;
		   
	   }
	   
	   //Start of Try/Catch
	   try {
		   
		   	//Open new FileOutputStream to place file
			outgoing = new FileInputStream(filePath);   
		 
			do {
				
				//Initialize receivePacket
				receivePacket = new DatagramPacket(ackData, ackData.length);	   
					
				//If it was an ACK response, WE SHOULD ALSO CHECK BLOCK NUMBER IN ACK TO MAKE SURE WE'RE SENDING THE RIGHT DATA
				if (ackData[0] == 0 && ackData[1] == 4) {
    		   
					//Data packet op code and block #
					data[0] = 0;
					data[1] = 3;
					data[2] = (byte) ((byte) blockNumber >> 8);
					data[3] = (byte) blockNumber;
					
					//Read the next set of bytes
					bytesRead = outgoing.read(dataSection);
					
					//Copy file data into packet
					System.arraycopy(dataSection, 0, data, 4, dataSection.length);
			
		    	   //Create new datagram packet to send
		    	   sendPacket = new DatagramPacket(data, data.length,
		                           InetAddress.getLocalHost(), receivePacket.getPort());
		    
		    	   //Send the packet to the server
		    	   sendReceiveSocket.send(sendPacket); 
		    	   
		    	   //Print the packet
		    	   System.out.println("Client: sending data packet");
		    	   System.out.println("To host: " + sendPacket.getAddress());
		    	   System.out.println("Destination host port: " + sendPacket.getPort());
		    	   System.out.println("Length: " + data.length);
		    	   System.out.println("Containing: ");
		    	   for (int j = 0; j < data.length; j++) {
		    		   System.out.println("byte " + j + " " + data[j]);
		    	   }
					
				} else {
    		   
					//Invalid response received
				}	
			
				//Close the FileInputStream
				outgoing.close();

				
			} while (bytesRead == Variables.MAX_PACKET_SIZE - DATA_PACKET_HEADER_SIZE);
		   
 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	   
   }
   
   /**
    * Send Acknowledgement to server saying we received
    * the block. 
    */
   public void sendACK(int blockNumber, int tidServer) {
	   DatagramPacket sendPacket;			//Acknowledgement packet being sent to the server
	   byte[] requestMsg = new byte[4];		//Message containing ack being sent to the server
	   
       //Start of Try/Catch
       try {
    	   
    	   //Request message (opcode = 2 bytes, block # = 2 bytes)
    	   requestMsg[0] = 0;
    	   requestMsg[1] = 4;
    	   requestMsg[2] = (byte) ((byte) blockNumber >> 8);
    	   requestMsg[3] = (byte) blockNumber;
    	   
    	   //Create new datagram packet to send
    	   sendPacket = new DatagramPacket(requestMsg, requestMsg.length,
                           InetAddress.getLocalHost(), tidServer);
    
    	   //Send the packet to the server
    	   sendReceiveSocket.send(sendPacket);
    	   
       //End of Try/Catch
       } catch (UnknownHostException e) {
    	   e.printStackTrace();
    	   System.exit(1);
    
       //End of Try/Catch
       } catch (IOException e) {
    	   e.printStackTrace();
    	   System.exit(1);	    	   
       }
   }
   
   
   public static void main(String args[]) {
      Client c = new Client();
      c.sendFile("test.txt");
   }
   
   
}
