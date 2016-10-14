package com.sysc.tftp.client;
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets
// the appropriate response from the server.
 
import java.io.*;
import java.net.*;
import java.util.Arrays;
import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class Client {

   private DatagramPacket sendPacket, receivePacket;   
   private DatagramSocket sendReceiveSocket; 

   public Client() {
   }

   /**
    * Create either a new read request or a new write request
    * which will be sent to the server before a file transfer
    * takes place.
    */
   private byte[] newRequest(byte rqType, String fileName) {
      byte[] msg,	// Message we send
              fn,   // Filename as an array of bytes
              md;   // Mode as an array of bytes

      int len;     	// Length of the message
      
      //Get filename as bytes
      fn = fileName.getBytes();
     
      //Get mode as bytes
      md = Variables.TRANSFER_MODE.getBytes();
     
      //Initialize packet data byte array
      msg = new byte[fn.length+md.length+4];
     
      //Set request type (RRQ = 1 or WRQ = 2)
      msg[0] = 0;
      msg[1] = rqType;
     
      //Copy filename into message
      System.arraycopy(fn,0,msg,2,fn.length);

      //Add 0 byte
      msg[fn.length+2] = 0;
      
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
   public void receiveFile(String fileName) {
		byte[] requestMsg; 	// Message containing request being sent to the server
		int sendPort; 		// Port we are sending the packet to
		int tid; 			// Random transfer ID generated
		Variables.Mode run = Variables.CLIENT_MODE; 
		DatagramPacket sendPacket; // A packet to send request to server
		
		String filePath = (Variables.CLIENT_FILES_DIR + fileName); //combine directory and filename to locate file		
		File f = new File(filePath); //create file object using the filePath above

		if (f.exists() && !f.isDirectory()) { 
			System.out.println("File already exists.");  //if the file already exists in the client directory, do not send RRQ and re-prompt user input
			return;										 //Note: do not send error code 6, rather terminate the request
		}
		
		// Start of Try/Catch
		try {

			// Generate random tid for client
			tid = 1 + (int) (Math.random() * ((10000) + 1));

			// Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);

			// If normal run mode use port 69, port 23 otherwise
			if (run == Variables.Mode.NORMAL)
				sendPort = Variables.NORMAL_PORT;
			else
				sendPort = Variables.TEST_PORT;

			// Generate byte array to send to server for request type and file
			requestMsg = newRequest((byte) 1, fileName);

			// Start of Try/Catch
			try {

				// Create new datagram packet to send
				sendPacket = new DatagramPacket(requestMsg, requestMsg.length, InetAddress.getLocalHost(), sendPort);

				// Write packet outgoing to log
				Logger.logPacketSending(sendPacket);

				// Send the packet to the server
				sendReceiveSocket.send(sendPacket);

				// Save the file to the client machine
				saveFileData(Variables.CLIENT_FILES_DIR + fileName);

				// End of Try/Catch
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);

				// End of Try/Catch
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// End of Try/Catch
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
		File f = new File(filePath); 	// File object for seeing if it already exists
		FileOutputStream incoming; 		// FileOutputStream for incoming data
		int currentBlock = 0; 			// Current block of data being received
		int currentBlockFromPacket = 0; // Current block # from the packet
		DatagramPacket receivePacket; 	// Incoming datagram packet
		byte[] packetData = new byte[Variables.MAX_PACKET_SIZE]; // Byte array for packet data
		byte[] fileData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE]; // Size of data in data packet

		// Start of Try/Catch
		try {

			// Check if file already exists
			if (f.exists() && !f.isDirectory()) {

				// File already exists
				System.out.println("The file already exists on the client, we will not overwrite it.");
				return;

			}

			

			// Initialize receivePacket
			receivePacket = new DatagramPacket(packetData, packetData.length);

			// While we have more packets to receive, loop
			do {

				// Increment block counter
				currentBlock++;

				// Receive an incoming packet
				sendReceiveSocket.receive(receivePacket);

				// Write packet outgoing to log
				Logger.logPacketReceived(receivePacket);

				// Start of Try/Catch
				try {

					// Check if data packet
					if (packetData[0] == 0 && packetData[1] == 3) {
						
						//System.out.println(filePath);
						
						//Reload the file
						f = new File(filePath);
						
						// Open new FileOutputStream to place file
						incoming = new FileOutputStream(f, true);

						// Extract block # from incoming packet
						int blockNumber = ((packetData[2] << 8) & 0xFF00) & (packetData[3] & 0xFF);

						// Grab file data from data packet
						fileData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
								receivePacket.getLength());

						// Write packet data to the file
						incoming.write(fileData);

						// Wait for data to be written to file before doing
						// anything else
						incoming.getFD().sync();

						// Send ACK for received block back to server
						sendACK(currentBlock, receivePacket.getPort());
						
						incoming.close();

					} else if (packetData[0] == 0 && packetData[1] == 5) {	//If an error code is received from server

						// Invalid op code
	
						fileData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
								receivePacket.getLength());
						
						String errorMsg = new String(fileData, "UTF-8");
						System.out.println(errorMsg);

			
					}

					// Sync failed, we could not save the bytes
				} catch (SyncFailedException e) {

					// Output error and return
					System.out.println("Could not save the full file! An error occured");
				//	incoming.close();
					return;

				}

			} while (!lastBlock(receivePacket));

			// Close FileOutputStream
		//	incoming.close();

			// End of Try/Catch
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

		// If datagram packet length is less than MAX_PACKET_SIZE its the last block
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
		byte[] requestMsg; // Message containing request being sent to the server
		int sendPort; // Port we are sending the packet to
		int tid; // Random transfer ID generated
		Variables.Mode run = Variables.CLIENT_MODE;
		DatagramPacket sendPacket; // A packet to send request to server

		// Start of Try/Catch
		try {

			// Generate random tid for client
			tid = 1 + (int) (Math.random() * ((10000) + 1));

			// Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);

			// If normal run mode use port 69, port 23 otherwise
			if (run == Variables.Mode.NORMAL)
				sendPort = Variables.NORMAL_PORT;
			else
				sendPort = Variables.TEST_PORT;

			// Start of Try/Catch
			try {

				// Generate byte array to send to server for request type and
				// file
				requestMsg = newRequest((byte) 2, fileName);

				// Create new datagram packet to send
				sendPacket = new DatagramPacket(requestMsg, requestMsg.length, InetAddress.getLocalHost(), sendPort);

				// Write packet outgoing to log
				Logger.logPacketSending(sendPacket);

				// Send the packet to the server
				sendReceiveSocket.send(sendPacket);

				// Send the file to the server
				sendFileData(Variables.CLIENT_FILES_DIR + fileName);

				// End of Try/Catch
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);

				// End of Try/Catch
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// End of Try/Catch
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
		File f = new File(filePath); 	// File object for seeing if it already exists
		FileInputStream outgoing; 		// FileOutputStream for outgoing data
		byte[] ackData = new byte[Variables.ACK_DATA_SIZE]; // Byte array for ack packet data
		byte[] data = new byte[Variables.MAX_PACKET_SIZE]; // Byte array for file data being sent
		byte[] incomingData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE];// Max size of the data in the packet
		byte[] dataSection;// Size of the data in the packet
		int blockNumber = 0; // Current block number being sent
		int bytesRead = 0; // Number of bytes read

		// Check if file already exists
		if (!f.exists() || f.isDirectory()) {

			// File already exists
			System.out.println("The file you are trying to send does not exist");
			return;

		}

		// Start of Try/Catch
		try {

			Logger.log("Sending file data...");

			// Open new FileOutputStream to place file
			outgoing = new FileInputStream(filePath);

			do {

				// Increment block number
				blockNumber++;

				// Initialize receivePacket
				receivePacket = new DatagramPacket(ackData, ackData.length);

				Logger.log("Client: Waiting for packet.");

				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);

				// Write packet incoming to log
				Logger.logPacketReceived(receivePacket);

				// If it was an ACK response, WE SHOULD ALSO CHECK BLOCK NUMBER
				// IN ACK TO MAKE SURE WE'RE SENDING THE RIGHT DATA
				
				if (ackData[0] == 0 && ackData[1] == 4) {

					Logger.log("Received ACK response");

					// Read the next set of bytes
					bytesRead = outgoing.read(incomingData);

					// Make new byte array to exact length
					dataSection = new byte[bytesRead];

					// Copy incoming data to dataSection byte array
					System.arraycopy(incomingData, 0, dataSection, 0, bytesRead);

					// Initialize packet to correct size
					data = new byte[bytesRead + Variables.DATA_PACKET_HEADER_SIZE];

					// Data packet op code and block #
					data[0] = 0;
					data[1] = 3;
					data[2] = (byte) ((byte) blockNumber >> 8);
					data[3] = (byte) blockNumber;

					// Copy file data into packet
					System.arraycopy(dataSection, 0, data, 4, dataSection.length);

					// Create new datagram packet to send
					sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(),
							receivePacket.getPort());

					// Send the packet to the server
					sendReceiveSocket.send(sendPacket);

					// Write packet outgoing to log
					Logger.logPacketSending(sendPacket);

				} else if(ackData[0] == 0 && ackData[1] == 5) {  //error packet received
					
					System.out.println("ERROR!!!!!!!!!!");
					
					bytesRead = outgoing.read();

					// Make new byte array to exact length
					dataSection = new byte[bytesRead];
					
					bytesRead = outgoing.read();
					
					System.arraycopy(incomingData, 0, dataSection, 0, bytesRead);
					
					String errorMsg = new String(dataSection, "UTF-8");
					System.out.println(errorMsg);

					// Invalid response received
				}

			} while (bytesRead == Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE);

			// Close the FileInputStream
			outgoing.close();

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
		DatagramPacket sendPacket; // Acknowledgement packet being sent to the server
		byte[] requestMsg = new byte[4]; // Message containing ack being sent to the server

		// Start of Try/Catch
		try {

			// Request message (opcode = 2 bytes, block # = 2 bytes)
			requestMsg[0] = 0;
			requestMsg[1] = 4;
			requestMsg[2] = (byte) ((byte) blockNumber >> 8);
			requestMsg[3] = (byte) blockNumber;

			// Create new datagram packet to send
			sendPacket = new DatagramPacket(requestMsg, requestMsg.length, InetAddress.getLocalHost(), tidServer);

			// Send the packet to the server
			sendReceiveSocket.send(sendPacket);

			// Write packet outgoing to log
			Logger.logPacketSending(sendPacket);

		// End of Try/Catch
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);

			// End of Try/Catch
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
  
}