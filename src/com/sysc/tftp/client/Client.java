package com.sysc.tftp.client;
// This class is the client side for a very simple assignment based on TFTP on

// UDP/IP. The client uses one port and sends a read or write request and gets
// the appropriate response from the server.

import java.io.*;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import com.sysc.tftp.utils.BlockUtil;
import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.TestLogger;
import com.sysc.tftp.utils.Variables;
import com.sysc.tftp.utils.Variables.Request;
import com.sysc.tftp.utils.VerifyUtil;

public class Client {

	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;

	private TestLogger clientLogger;

	public Client() {
		clientLogger = new TestLogger(this);
	}

	/**
	 * Create either a new read request or a new write request which will be
	 * sent to the server before a file transfer takes place.
	 */
	private byte[] newRequest(byte rqType, String fileName) {
		byte[] msg, // Message we send
				fn, // Filename as an array of bytes
				md; // Mode as an array of bytes

		int len; // Length of the message

		// Get filename as bytes
		fn = fileName.getBytes();

		// Get mode as bytes
		md = Variables.TRANSFER_MODE.getBytes();

		// Initialize packet data byte array
		msg = new byte[fn.length + md.length + 4];

		// Set request type (RRQ = 1 or WRQ = 2)
		msg[0] = 0;
		msg[1] = rqType;

		// Copy filename into message
		System.arraycopy(fn, 0, msg, 2, fn.length);

		// Add 0 byte
		msg[fn.length + 2] = 0;

		// Add mode bytes to message
		System.arraycopy(md, 0, msg, fn.length + 3, md.length);

		// Get length of message
		len = fn.length + md.length + 4; // length of the message

		// Finish message with another 0 byte
		msg[len - 1] = 0;

		// Return message request as bytes
		return msg;
	}

	/**
	 * Returns the packet to initiate a write request on a specified file
	 * @throws UnknownHostException 
	 */
	private DatagramPacket createRequestPacket(String fileName, int requestType) throws UnknownHostException {
		
		byte[] requestMsg; // Message containing request being sent to the server
		Variables.Mode run = Variables.CLIENT_MODE;
		int sendPort; //Port to send initial request packet on
		
		// Generate byte array to send to server for request type and file
		requestMsg = newRequest((byte) requestType, fileName);

		// If normal run mode use port 69, port 23 otherwise
		if (run == Variables.Mode.NORMAL) {
			sendPort = Variables.NORMAL_PORT;
		} else { 
			sendPort = Variables.TEST_PORT;
		}
		
		// Create new datagram packet to send
		sendPacket = new DatagramPacket(requestMsg, requestMsg.length, Variables.serverIP, sendPort);
		
		//Return request packet
		return sendPacket;
		
	}	
	
	/**
	 * Receive a file from the server based on the supplied filename.
	 */
	public void receiveFile(String fileName) {
		int tid; // Random transfer ID generated
		DatagramPacket sendPacket; // A packet to send request to server

		// combine directory and filename to locate file
		String filePath = (Variables.CLIENT_FILES_DIR + fileName);

		// create file object using the filePath above
		File f = new File(filePath);

		if (f.exists() && !f.isDirectory()) {
			System.out.println("File already exists."); // if the file already exists in the client directory, do not
														// send RRQ and re-prompt user input
			
			// Note: do not send error code 6, rather terminate the request
			return; 
		}

		// Start of Try/Catch
		try {

			// Generate random tid for client
			tid = 1 + (int) (Math.random() * ((10000) + 1));

			// Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);
			
			//Specify the timeout for the socket
			sendReceiveSocket.setSoTimeout(Variables.packetTimeout);

			// Start of Try/Catch
			try {

				// Create new datagram packet to send
				sendPacket = createRequestPacket(fileName, 1);

				// Write packet outgoing to log
				Logger.logRequestPacketSending(sendPacket);
				
				clientLogger.archive(true, sendPacket);

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
			e1.printStackTrace();
		}

	}

	/**
	 * Saves incoming file packets to the specified path. Also sends an ack back
	 * to the server for each packet received.
	 */
	private void saveFileData(String filePath) {
		File f = new File(filePath); // File object for seeing if it already exists
		OutputStream incoming = null;
		int currentBlock = 0; // Current block of data being received
		int currentBlockFromPacket = 0; // Current block # from the packet
		DatagramPacket receivePacket; // Incoming datagram packet
		byte[] packetData = new byte[Variables.MAX_PACKET_SIZE]; // Byte array for packet data
		byte[] fileData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE]; // Size of data in data packet
		int timeouts = 0;	//Number of timeouts which have occured
		String fileName; 	//Filename of file being sent
		int fromPort = 0;	//Port we are receiving packets from
		
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
				
				//Start of Try/Catch
				try { 
					
					// Increment block counter
					currentBlock++;
	
					// Receive an incoming packet
					sendReceiveSocket.receive(receivePacket);
					
					clientLogger.archive(false, receivePacket);
					
					// Write packet outgoing to log
					Logger.logPacketReceived(receivePacket);		
					
					//If we havn't set the port we're receiving from yet
					if (fromPort == 0) {
						
						//Set from port as port we are receiving first packet from
						fromPort = receivePacket.getPort();
						
						//Log the from port 
						Logger.log("We are receiving from port " + fromPort);	
						
					}
					
					//Check incoming packet port with port we are use to receiving from
					if (fromPort != receivePacket.getPort()) {
						
						//Log packet received from bad source
						Logger.log("Received packet from unknown source port...");
						currentBlock--;
						//If ports don't match up, skip this packet
						continue;
						
					}
					
					//Check incoming packet is formatted properly
					if (VerifyUtil.verifyRequest(packetData, packetData.length) != Request.DATA 
							&& VerifyUtil.verifyRequest(packetData, packetData.length) != Request.ERROR) {
						
						//Not the right request, break
						System.out.println("Invalid incoming request recevied, terminating.");
						
						//Exit file save loop
						break;
					
					}
					
					//Reset timeout counter
					timeouts = 0;	
					
					// Check if data packet
					if (packetData[0] == 0 && packetData[1] == 3) {

						// Reload the file
						f = new File(filePath);
						
						Path p = Paths.get(filePath);

						// Extract block # from incoming packet
						byte[] block = new byte[2];
						block[0] = packetData[2];
						block[1] = packetData[3];
						currentBlockFromPacket = BlockUtil.byteToInt(block);
						
						//If incoming block is the block we expected
						if (currentBlockFromPacket == currentBlock ) {
							
							// Grab file data from data packet
							fileData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
									receivePacket.getLength());
	
							// Write packet data to the file
							try {
								
								//Open new output stream for data
								incoming = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND); 

								//Write the data to the file
								incoming.write(fileData);	//throws FileAlreadyExistsException, IOException
								
								// Send ACK for received block back to server
								sendACK(currentBlock, receivePacket.getPort());
	
								//Close file output stream
								incoming.close();
							
							//Tried to write to directory without write permissions
							} catch (AccessDeniedException e) {  
								
								//Print error message 
								System.out.println("Access Violation.");
								
								//Exit save file data loop
								break;
								
							//Tried to write a file that already exists	
							} catch (FileAlreadyExistsException e2) {  
								
								//Print error message
								System.out.println("File Already Exists.");
								
								//Exit save file data loop
								break;
								
							//Insufficient disk space for the file transfer	
							} catch (IOException e3) {	
								
								try {
									Files.delete(p);
								} catch (IOException e4) {
									break;
								}
								//Print error message
								System.out.println("Disk Full.");
								
								//Exit save file data loop
								break;
								
							} finally {
								if(incoming!=null) {
									incoming.close();
								}
							}
						//Received wrong data packet, re-send correct ACK
						} else {
							
							//Log wrong packet received
							Logger.log("Client: Received block #" + currentBlockFromPacket + ", Expected block #" + (currentBlock - 1) + " (resending ACK)");
							
							//Reduce block counter
							currentBlock --;
							
							// Send ACK for data block we didn't want
							sendACK(currentBlockFromPacket, receivePacket.getPort());
							
						}
					
					//Error packet received from server
					} else if (packetData[0] == 0 && packetData[1] == 5) {

						//Get data from error packet
						fileData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
								receivePacket.getLength());

						//Get error message from packet data 
						String errorMsg = new String(fileData, "UTF-8");
						
						//Print the error out
						System.out.println(errorMsg);
						
						//We terminate for any error code other than 5
						if (packetData[3] != 5) {
							
							//Log termination
							Logger.log("Terminating because we received error code " + packetData[3] + " from server");
							
							//Break
							break;
							
						}
						
					}
				
				//Timeout occured
				} catch (SocketTimeoutException e) {
					
					//Check if timeout limit reached
					if (timeouts >= Variables.packetRetransmits) {
						
						//Log the error
						Logger.log("Too many timeouts, cancelling receiving the file.");
						System.exit(1);
						
					}
							
					//Increment timeouts counter
					timeouts ++;
					
					//Reduce block counter
					currentBlock --;
					
					//If it was the initial request packet we need to resend, format it
					if (sendPacket == null) {
						
						//Get filename of file being sent
						fileName = new File(filePath).getName();
						
						// Create write request packet
						sendPacket = createRequestPacket(fileName, 1);
						
					} 
					
					// Send ACK for previous received block
					sendACK(currentBlock, receivePacket.getPort());
					
				}

			} while (!lastBlock(receivePacket));

			// End of Try/Catch
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Checks the length of a datagram packet. If it is less than 512 bytes it
	 * is the last packet for the file.
	 */
	public boolean lastBlock(DatagramPacket datagramPacket) {
		
		// If datagram packet length is less than MAX_PACKET_SIZE it's the last block
		if (datagramPacket.getLength() < Variables.MAX_PACKET_SIZE) {
			
			//It is the last block
			return true;
		} else {
			
			//Not the last block
			return false;
		}
		
	}

	/**
	 * Send a file to the server based on the supplied filename.
	 */
	public void sendFile(String fileName) {
		int tid; // Random transfer ID generated
		DatagramPacket sendPacket; // A packet to send request to server
		File f = new File(Variables.CLIENT_FILES_DIR + fileName); // File object for seeing if it already exists

		// Check if file already exists
		if (!f.exists() || f.isDirectory()) {
			
			// File already exists
			System.out.println("The file you are trying to send '" + fileName + "' does not exist");
			return;
			
		}

		// Start of Try/Catch
		try {

			// Generate random tid for client
			tid = 1 + (int) (Math.random() * ((10000) + 1));

			// Open new datagram socket
			sendReceiveSocket = new DatagramSocket(tid);
			
			//Specify the timeout for the socket
			sendReceiveSocket.setSoTimeout(Variables.packetTimeout);

			// Start of Try/Catch
			try {
				
				// Create new datagram packet for write request
				sendPacket = createRequestPacket(fileName, 2);

				// Write packet outgoing to log
				Logger.logRequestPacketSending(sendPacket);
				clientLogger.archive(true, sendPacket);
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
			e1.printStackTrace();
		}
		
	}

	/**
	 * Sends outgoing file packets containing the file data to the server.
	 */
	private void sendFileData(String filePath) {
		FileInputStream outgoing; // FileOutputStream for outgoing data
		byte[] incomingPacket = new byte[Variables.MAX_PACKET_SIZE]; // Byte array for ack packet data
		byte[] data = new byte[Variables.MAX_PACKET_SIZE]; // Byte array for file data being sent
		byte[] outgoingData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE]; // Max size of the data in the packet
		byte[] incomingData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE]; // Max size of the data in the packet
		byte[] dataSection;// Size of the data in the packet
		int blockNumber = 0; // Current block number being sent
		int bytesRead = 0; // Number of bytes read
		int timeouts = 0;	//Number of timeouts which have occured
		int currentBlockFromPacket = 0;	//Current block number from incoming packet
		boolean lastBlock = false;
		String fileName; 	//Filename of file being sent
		int highestACK = 0;	//Highest ACK'd block number
		int  fromPort = 0;	//Port we are receiving data from on server
		
		// Start of Try/Catch
		try {

			Logger.log("Sending file data...");

			// Open new FileOutputStream to place file
			outgoing = new FileInputStream(filePath);
			
			//Loop until last block breaks this
			while(true) {
				
				// Increment block number
				blockNumber++;

				// Initialize receivePacket
				receivePacket = new DatagramPacket(incomingPacket, incomingPacket.length);

				Logger.log("Client: Waiting for packet.");

				// Start of Try/Catch
				try {
					// Block until a datagram is received via sendReceiveSocket.
					sendReceiveSocket.receive(receivePacket);
					
					clientLogger.archive(false, receivePacket);
					// Write packet incoming to log
					Logger.logPacketReceived(receivePacket);
	
					//If we havn't set the port we're receiving from yet
					if (fromPort == 0) {
						
						//Set from port as port we are receiving first packet from
						fromPort = receivePacket.getPort();
						
					}
					
					//Check incoming packet port with port we are use to receiving from
					if (fromPort != receivePacket.getPort()) {
						
						//Log packet received from unknown source
						Logger.log("Received packet from unknown source port");
						blockNumber--;
						//If ports don't match up, skip this packet
						continue;
						
					}
					
					//Check incoming packet is formatted properly
					if (VerifyUtil.verifyRequest(incomingPacket, incomingPacket.length) != Request.ACK 
							&& VerifyUtil.verifyRequest(incomingPacket, incomingPacket.length) != Request.ERROR) {
						
						//Not the right request, break
						System.out.println("Invalid incoming request recevied, terminating.");
						
						//Exit file save loop
						break;
					
					}
					
					//Reset timeout counter
					timeouts = 0;
						
					// If it was an ACK response, WE SHOULD ALSO CHECK BLOCK NUMBER
					// IN ACK TO MAKE SURE WE'RE SENDING THE RIGHT DATA
	
					if (incomingPacket[0] == 0 && incomingPacket[1] == 4) {
	
						Logger.log("Received ACK response");
						
						// Extract block # from incoming packet
						byte[] block = new byte[2];
						block[0] = incomingPacket[2];
						block[1] = incomingPacket[3];
						currentBlockFromPacket = BlockUtil.byteToInt(block);
						
						Logger.log( "Current blockNumber " + blockNumber);
						
						//Check if the correct block
						if ( currentBlockFromPacket == (blockNumber - 1) ) {
							
							// Last block is sent and this is the ack... break, file transfer over
							if(lastBlock == true){	
								
								//in the event we write a small file (<1 block) to a server-side directory without write permissions, we have to listen for one more response from the server (so that the server can attempt to write the file)
								break;
								
							}
							
							//Save highest ACK'd packet
							highestACK = currentBlockFromPacket;
							
							// Read the next set of bytes
							bytesRead = outgoing.read(outgoingData);
		
							// this a last minute fix, should be properly handled later
							if (bytesRead <= 0) {
								dataSection = new byte[0];
								bytesRead = 0;
							} else {
								// Make new byte array to exact length
								dataSection = new byte[bytesRead];
							}
		
							// Copy incoming data to dataSection byte array
							System.arraycopy(outgoingData, 0, dataSection, 0, bytesRead);
		
							// Initialize packet to correct size
							data = new byte[bytesRead + Variables.DATA_PACKET_HEADER_SIZE];
		
							// Data packet op code and block #
							data[0] = 0;
							data[1] = 3;
							
							block = BlockUtil.intToByte(blockNumber);
							data[2] = block[0];
							data[3] = block[1];
		
							// Copy file data into packet
							System.arraycopy(dataSection, 0, data, 4, dataSection.length);
		
							// Create new datagram packet to send
							sendPacket = new DatagramPacket(data, data.length, Variables.serverIP,
									receivePacket.getPort());
		
							clientLogger.archive(true, sendPacket);
							// Send the packet to the server
							sendReceiveSocket.send(sendPacket);
		
							// Write packet outgoing to log
							Logger.logPacketSending(sendPacket);
						
						//Sorcerer's Apprentice Avoidance
						} else {
							
							Logger.log("Highest ACK : " + highestACK);
							
							//We should resend the last block because it was lost
							if (currentBlockFromPacket == highestACK) {
								
								//Log that packet was lost
								Logger.log("Looks like a data packet was lost, resending...");
								
								clientLogger.archive(true, sendPacket);
								
								// Send the packet to the server
								sendReceiveSocket.send(sendPacket);
								
								// Write packet outgoing to log
								Logger.logPacketSending(sendPacket);	
								
							} else {
							
								//Log ignore to logger
								Logger.log("Ignoring ACK with block number " + currentBlockFromPacket + ", we are passed this.");
							
							}
							
							//Decrement block number 
							blockNumber--;							
							
						}
			
					//Error packet received
					} else if (incomingPacket[0] == 0 && incomingPacket[1] == 5) { 

						// Get the message from the error packet
						incomingData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
								receivePacket.getLength());
	
						// Convert byte array to UTF8 string
						String errorMsg = new String(incomingData, "UTF-8");
	
						// Print error msg
						System.out.println(errorMsg);
	
						//Log termination
						Logger.log("Terminating because we received error code " + incomingPacket[3] + " from server");
						
						//Break
						break;
						
					}
					
					//Check if the next block is the last one
					if ( !(bytesRead == Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE) ) {
						
						//Specify the next block is the last one
						lastBlock = true;
						
					}
				
				//Timeout occured
				} catch (SocketTimeoutException e) {
					
					//Log timeout
					Logger.log("Timeout occured.");
					
					//Check if timeout limit reached
					if (timeouts >= Variables.packetRetransmits) {
						
						//Log the error
						Logger.log("Too many timeouts, cancelling receiving the file.");
						System.exit(1);
						
					}
					
					//Log resending data packet
					Logger.log("Resending packet");
					
					//Increment timeouts counter
					timeouts ++;
					
					//Reduce block counter
					blockNumber --;

					//If it was the initial request packet we need to resend, format it
					if (sendPacket == null) {
						
						//Get filename of file being sent
						fileName = new File(filePath).getName();
						
						// Create write request packet
						sendPacket = createRequestPacket(fileName, 2);
						
					} 
					clientLogger.archive(true, sendPacket);
					// Resend the last packet to the server
					sendReceiveSocket.send(sendPacket);

					// Write packet outgoing to log
					Logger.logPacketSending(sendPacket);
					
				}
				
			}
			
			// Close the FileInputStream
			outgoing.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send Acknowledgement to server saying we received the block.
	 */
	public void sendACK(int blockNumber, int tidServer) {
		DatagramPacket sendPacket; // Acknowledgement packet being sent to the server
		byte[] requestMsg = new byte[4]; // Message containing ACK being sent to the server

		// Start of Try/Catch
		try {

			// Request message (opcode = 2 bytes, block # = 2 bytes)
			requestMsg[0] = 0;
			requestMsg[1] = 4;
			
			byte[] block = BlockUtil.intToByte(blockNumber);
			requestMsg[2] = block[0];
			requestMsg[3] = block[1];

			// Create new datagram packet to send
			sendPacket = new DatagramPacket(requestMsg, requestMsg.length, Variables.serverIP, tidServer);
			
			clientLogger.archive(true, sendPacket);

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
	
	public DatagramPacket getReceivePacket() {
		return receivePacket;
	}
	
	public TestLogger getLogger() {
		return clientLogger;
	}
}
