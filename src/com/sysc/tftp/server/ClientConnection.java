package com.sysc.tftp.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import com.sysc.tftp.utils.BlockUtil;
import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;
import com.sysc.tftp.utils.Variables.Request;
import com.sysc.tftp.utils.VerifyUtil;

public class ClientConnection implements Runnable {

	// UDP datagram packets and socket used to send / receive
	private DatagramPacket receivePacket = null, sendPacket = null;
	private DatagramSocket sendReceiveSocket = null;

	private byte[] data = null; 		// holds the original request
	private byte[] fileBytes = null;	// hold bytes of file to transfer
	private int blockNumber = 0; 		// current block of data being received/sent
	String filename = null;				// Filename of file under consideration
	
	// client information: port, IP, length of data
	private int len = 0, clientPort = 0;
	private InetAddress clientIP = null;

	private boolean errorDetected = false; 	// flag set when error detected,
											// signals closing thread
	
	public ClientConnection(byte[] data, int len, InetAddress ip, int port) {
		this.data = data;
		this.len = len;
		this.clientIP = ip;
		this.clientPort = port;
		this.blockNumber = 0;
		try {
			// Initialize datagram socket
			sendReceiveSocket = new DatagramSocket();
			// Set socket timeout for receiving 
			sendReceiveSocket.setSoTimeout(Variables.packetTimeout);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Handles request
	 */
	@Override
	public void run() {
		byte[] response = null;
		int timeouts = 0;				//Counter for timeouts
		int currentBlockFromPacket = 0;	//Current block number from incoming packet
		int fromPort = 0;				//Port we are receiving from
		Request incomingRequestType = null;	//Request type extracted from incoming packet
		byte[] fileData = new byte[Variables.MAX_PACKET_SIZE - Variables.DATA_PACKET_HEADER_SIZE]; // Size of data in data packet

		//Verify incoming request type
		Request req = VerifyUtil.verifyInitialRequest(data, len);
		
		if (req == null || req == Request.ERROR) {
			Logger.log("Invalid request");
			
			response = packageError(Variables.ERROR_4);
			sendPacket = new DatagramPacket(response, response.length, clientIP, clientPort);
			
			Logger.logPacketSending(sendPacket);

			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			Logger.log("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
			Logger.log("");
			return;
		}

		filename = Variables.SERVER_FILES_DIR + pullFilename(data);
		File f = new File(filename);

		//Read request
		if (req == Request.RRQ) {
			
			if (fileBytes == null) {

				fileBytes = new byte[(int) f.length()];

				// Attempt to open the file
				try {
					
					//Open file input stream with requested file 
					FileInputStream fis = new FileInputStream(filename);
					
					//Read x bytes from file 
					fis.read(fileBytes);
					
					//Close stream
					fis.close();
			
				//Thrown when file not found 
				} catch (Exception e) {
					
					//Error detected
					errorDetected = true;
					
					//File not found error
					response = packageError(Variables.ERROR_1);
					
				}

				//If error flag set
				if (!errorDetected) {
					
					//Increment block number
					blockNumber++;
					
					//Get file data to send
					response = packageRead();
					
				}
				
			}
			
		//Write request
		} else if (req == Request.WRQ) {
			
			// Client requesting to write a file that already exists
			if (f.exists() && !f.isDirectory()) {
				
				// Form the error message response
				response = packageError(Variables.ERROR_6);
				
				// Set error detected flag
				errorDetected = true;
				
			} else {
				
				// Valid write request: format ACK response
				byte[] block = BlockUtil.intToByte(blockNumber);
				Variables.ACK[2] = block[0];
				Variables.ACK[3] = block[1];
				response = Variables.ACK;
				
			}
			
		}

		//Create datagram packet to send 
		sendPacket = new DatagramPacket(response, response.length, clientIP, clientPort);
		//If we havn't set the port we're receiving from yet
		if (fromPort == 0) {
			
			// Set from port as port we are receiving first packet from
			fromPort = clientPort;
			
			//Log the from port 
			Logger.log("We are receiving from port " + fromPort);				
			
		}
		
		//Log packet sending
		Logger.logPacketSending(sendPacket);

		// Start of Try/Catch
		try {
						
			//Send packet 
			sendReceiveSocket.send(sendPacket);
			
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		Logger.log("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
		Logger.log("");

		// close sendReceiveSocket after sending error datagram
		if (errorDetected) {
			Logger.log("Error detected, closing thread.");
			sendReceiveSocket.close();
		}

		if (req == Request.RRQ && fileBytes != null && response.length < Variables.MAX_PACKET_SIZE) {
			fileBytes = null;
			// We're finished with this socket, so close it.
			Logger.log("Closing socket...");
			sendReceiveSocket.close();
			Logger.log("Thread done.");
			return;
		}

		//Loop until break
		while (true) {
			
			//If an error packet was sent
			if (errorDetected) {
				
				//Break while loop
				break;
			}

			//Initialize byte array for incoming data
			byte[] received = new byte[Variables.MAX_PACKET_SIZE];
			
			//Initialize new packet to receive
			receivePacket = new DatagramPacket(received, received.length);
			
			//Start of Try/Catch
			try { 
				
			
				// Start of Try/Catch
				try {
					
					// Block until a datagram is received via sendReceiveSocket.
					sendReceiveSocket.receive(receivePacket);
				
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
					Logger.log("Resending data packet");
					
					//Increment timeouts counter
					timeouts ++;
					
					//Resend previous packet
					sendReceiveSocket.send(sendPacket);
					
					//Go back to start of while loop
					continue;
				
				}
	
			//Error with socket
			} catch (IOException e) {
				
				e.printStackTrace();
				System.exit(1);
				
			}
			
			
			//If we got here we received the packet, no timeouts
			
			//Reset timeouts to 0
			timeouts = 0;
			
			// Process the received datagram.
			Logger.logPacketReceived(receivePacket);

			//Check incoming packet port with port we are use to receiving from
			if (fromPort != receivePacket.getPort()) {
				
				//Log packet received from bad source
				Logger.log("Received packet from unknown source port...");
				
				// Form the error message response, not set error flag because we want to continue
				response = packageError(Variables.ERROR_5);
				//Create new response packet 
				sendPacket = new DatagramPacket(response, response.length, clientIP, receivePacket.getPort());
				
				//Log we are sending this packet
				Logger.logPacketSending(sendPacket);

				//Start of Try/Catch
				try {
					
					//Send the packet
					sendReceiveSocket.send(sendPacket);
					
				//Error sending packet
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				//Log the server sent the packet successfully
				Logger.log("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
				Logger.log("");
				continue;
			
			//Packet is from the correct source!
			} else {
			
				incomingRequestType = VerifyUtil.verifyRequest(received,  received.length);
				
				//Received error packet
				if (incomingRequestType == Request.ERROR) {
					
					//Get data from error packet
					fileData = Arrays.copyOfRange(receivePacket.getData(), Variables.DATA_PACKET_HEADER_SIZE,
							receivePacket.getLength());

					//Get error message from packet data 
					String errorMsg = "";
					
					//Start of Try/Catch
					try {
						
						//Extract error message
						errorMsg = new String(fileData, "UTF-8");
						
					//Caught exception
					} catch (UnsupportedEncodingException e) {
						
						//Print exception
						e.printStackTrace();
						
					}
					
					//Print the error out
					System.out.println(errorMsg);
					
					//We terminate for any error code other than 5
					if (received[3] != 5) {
						
						//Log termination
						Logger.log("Terminating because we received error code " + received[3] + " from server");
						
						//Break
						break;
						
					}
					
				}
				
				//Get block number from packet
				byte[] block = new byte[2];
				block[0] = received[2];
				block[1] = received[3];
				currentBlockFromPacket = BlockUtil.byteToInt(block);
	
				//If read request
				if (req == Request.RRQ) {
					
					//Verify the incoming packet is an ACK, we dont want anything else
					if (incomingRequestType != Request.ACK) {
						
						// Form the error message response
						response = packageError(Variables.ERROR_4);
						
						// Set error detected flag
						errorDetected = true;
						
					//Incoming packet is an ACK!
					} else {
						
						//Verify incoming ACK
						if (verifyACK(received)) {
							
							//Generate appropriate response
							response = packageRead();
						
						//Sorcerer's Apprentice Avoidance
						} else {
		
							//Log ignore to logger
							Logger.log("Ignoring ACK with block number " + currentBlockFromPacket);
							
							//Continue to next received packet
							continue;
						}
						
						
					}
							
				//If write request
				} else if (req == Request.WRQ) {
					
					
					//Verify the incoming packet is DATA, we dont want anything else
					if (incomingRequestType != Request.DATA) {
						
						// Form the error message response
						response = packageError(Variables.ERROR_4);
						
						// Set error detected flag
						errorDetected = true;
					
					//Incoming packet is DATA
					} else {
						
						//Very the incoming data
						if (verifyDATA(received)) {
							
							//Start of Try/Catch
							try {
								
								//Write data to file 
								response = writeToFile(filename,
										Arrays.copyOfRange(received, Variables.DATA.length, received.length));
							
							//Error writting to file
							} catch (Throwable e) {
								
								e.printStackTrace();
								
							}
						
						//Timeout receiving ACK on other end must have occured
						} else {
							
							//Log that we received unknown packet
							Logger.log("Unexpected packet received, ignoring...");
							
							//ACK the data packet
							response = new byte[Variables.ACK.length];	
							
							//Copy ACK packet type into package
							System.arraycopy(Variables.ACK, 0, response, 0, 2);

							//Copy block number into package
							response[2] = block[0];
							response[3] = block[1];
							
							
							//If we get here we received a data block from the past that may have been delayed / re-transmitted
							//Send an ACK for it anyways
							
						}
						
					}
					
					
				}

			}
			
			//Create new response packet 
			sendPacket = new DatagramPacket(response, response.length, clientIP, clientPort);
			
			//Log we are sending this packet
			Logger.logPacketSending(sendPacket);

			//Start of Try/Catch
			try {
				
				//Send the packet
				sendReceiveSocket.send(sendPacket);
				
			//Error sending packet
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			//Log the server sent the packet successfully
			Logger.log("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
			Logger.log("");

			//If read request and we are done
			if (req == Request.RRQ && fileBytes != null && response.length < Variables.MAX_PACKET_SIZE) {
				fileBytes = null;
				break;
				
			//If write request and we are done 
			} else if (req == Request.WRQ && receivePacket.getLength() < Variables.MAX_PACKET_SIZE) {
				break;
			}
			
		}

		// We're finished with this socket, so close it.
		Logger.log("Closing socket...");
		sendReceiveSocket.close();
		Logger.log("Thread done.");
		
	}

	/**
	 * Packages file to be read
	 * 
	 * @return Part, if not all, of file
	 */
	public byte[] packageRead() {
		byte[] finalPackage = null;	//Datagram bytes to return
		byte[] dataPackage = new byte[Variables.DATA.length];	//Data portion of the datagram
		
		System.arraycopy(Variables.DATA, 0, dataPackage, 0, 2);	//Copy data packet type to array
		
		byte[] block = BlockUtil.intToByte(blockNumber);
		dataPackage[2] = block[0];
		dataPackage[3] = block[1];
		
		//This isn't the last packet
		if (fileBytes.length > Variables.MAX_PACKET_SIZE - Variables.DATA.length) {
			
			//Initialize new final packet to return
			finalPackage = new byte[Variables.MAX_PACKET_SIZE];
			
			//Copy packet type into packet being returned
			System.arraycopy(dataPackage, 0, finalPackage, 0, Variables.DATA.length);
			
			//Copy packet data into packet being returned
			System.arraycopy(fileBytes, 0, finalPackage, Variables.DATA.length,
					Variables.MAX_PACKET_SIZE - Variables.DATA.length);
			
			//Update current position in file 
			fileBytes = Arrays.copyOfRange(fileBytes, Variables.MAX_PACKET_SIZE - Variables.DATA.length,
					fileBytes.length);
			
		//This is the last packet
		} else {
			
			//Initialize new final packet to return
			finalPackage = new byte[Variables.DATA.length + fileBytes.length];
			
			//Copy packet type into packet being returned
			System.arraycopy(dataPackage, 0, finalPackage, 0, Variables.DATA.length);
			
			//Copy packet data into packet being returned			
			System.arraycopy(fileBytes, 0, finalPackage, Variables.DATA.length, fileBytes.length);
			
			//Update current position in file 
			fileBytes = new byte[0];
		}
		
		//Return entire data packet
		return finalPackage;
		
	}

	/**
	 * Writes to specified file, if file doesn't exist create new file
	 * 
	 * @param filename
	 *            File to find/create and write to
	 * @param fileContent
	 *            Content to put in file
	 */
	public byte[] writeToFile(String filename, byte[] fileContent) throws Throwable {
	
		Path p = null;
		
		OutputStream out = null;
		
		//Start of Try/Catch
		try {
			//Get path to filename
			p = Paths.get(filename);
			
			//Open a new output stream
			out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND); 
			
			//Write the new data to the file 
			out.write(fileContent);	
			
		} catch (AccessDeniedException e) {  //tried to write to directory without write permissions, return access violation
			errorDetected = true;
			return packageError(Variables.ERROR_2);
		} catch (FileAlreadyExistsException e2) {  //tried to write a file that already exists, return file already exists
			errorDetected = true;
			return packageError(Variables.ERROR_6);
		} catch (IOException e3) {					//insufficient disk space for the file transfer, return disk full	
			
			try {
				Files.delete(p);						//delete the *empty* file
			} catch (IOException e4) {
			}
			errorDetected = true;
			return packageError(Variables.ERROR_3);
		} finally {
			//Close output stream
			if(out!=null) {
				out.close();
			}
		}
		
		// write successful.. return ACK
		byte[] ackPackage = new byte[Variables.ACK.length];	
		
		//Copy ACK packet type into package
		System.arraycopy(Variables.ACK, 0, ackPackage, 0, 2);

		//Copy block number into package
		byte[] block = BlockUtil.intToByte(blockNumber);
		ackPackage[2] = block[0];
		ackPackage[3] = block[1];
		
		//return ACK package
		return ackPackage;
	}

	/**
	 * Verifies that the first 4 bytes are ACK
	 * 
	 * @param data
	 *            Message received
	 * @return if proper ACK message
	 */
	public boolean verifyACK(byte[] data) {
		byte[] block = BlockUtil.intToByte(blockNumber);
		Variables.ACK[2] = block[0];
		Variables.ACK[3] = block[1];
		for (int i = 0; i < Variables.ACK.length; i++) {
			if (Variables.ACK[i] != data[i]) {
				return false;
			}
		}
		
		blockNumber++;
		return true;
	}

	/**
	 * Verifies that the first 4 bytes are DATA
	 * 
	 * @param data
	 *            Message received
	 * @return if proper DATA message
	 */
	public boolean verifyDATA(byte[] data) {
		
		if (data.length <= Variables.DATA.length) { // no data in message
			return false;
		}
		
		//Increment current block number 
		blockNumber++;
		
		//Set what the data packet block numbers should be 
		byte[] block = BlockUtil.intToByte(blockNumber);
		Variables.DATA[2] = block[0];
		Variables.DATA[3] = block[1];
		
		//Loop each element of data block characteristics
		for (int i = 0; i < Variables.DATA.length; i++) {
			
			//Match data packet with what is expected
			if (Variables.DATA[i] != data[i]) {
				
				//Not a valid data block, so decrement the block counter
				blockNumber--;
				
				//Invalid block
				return false;
				
			}
		}
		
		//Valid block
		return true;
	}

	/**
	 * Returns the name of the file
	 * 
	 * @param data
	 *            Contains filename
	 * @return filename
	 */
	public String pullFilename(byte[] data) {
		int j;
		for (j = 2; j < len; j++) {
			if (data[j] == 0)
				break;
		}
		return new String(data, 2, j - 2);
	}

	// formulate the error packet: [05|ErrorCode|ErrMsg|0] (all in bytes)
	public byte[] packageError(byte[] error) {

		String errorMessage = new String();
		byte[] errorBytes;
		byte[] zeroByte = { (byte) 0 };

		if (error == Variables.ERROR_1) {
			errorMessage = "File '"+ filename + "' not found.";
		}

		else if (error == Variables.ERROR_2) {
			errorMessage = "Access Violation.";
		}

		else if (error == Variables.ERROR_3) {
			errorMessage = "Disk Full.";
		}

		else if (error == Variables.ERROR_6) {
			errorMessage = "File '"+ filename + "' already exists.";
		}

		errorBytes = errorMessage.getBytes();

		byte[] errorPacket = new byte[error.length + errorBytes.length + zeroByte.length];

		System.arraycopy(error, 0, errorPacket, 0, 4);
		errorBytes = errorMessage.getBytes();
		System.arraycopy(errorBytes, 0, errorPacket, error.length, errorBytes.length);
		System.arraycopy(zeroByte, 0, errorPacket, (error.length + errorBytes.length), zeroByte.length);
		return errorPacket;

	}

}
