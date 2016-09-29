package com.sysc.tftp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.sysc.tftp.utils.Variables;
import com.sysc.tftp.utils.Variables.Request;

public class ClientConnection implements Runnable {

	private DatagramPacket receivePacket = null, sendPacket = null;
	private DatagramSocket sendReceiveSocket = null;
	
	private byte[] data = null;
	private int len = 0, clientPort = 0;
	private InetAddress clientIP = null;

	public ClientConnection(byte[] data, int len, InetAddress ip, int port) {
		this.data = data;
		this.len = len;
		this.clientIP = ip;
		this.clientPort = port;
	}

	@Override
	public void run() {
		long threadId = Thread.currentThread().getId(); // for printing, to
														// show which thread
														// is doing what
		byte[] response = null;
		String filename = null;

		Request req = verifyRequest(data);
		if (req == null || req == Request.ERROR) {
			// TODO
			// issue (iteration 2)
		}
		
		filename = pullFilename(data);
		
		// Create a response.
		if (req == Request.RRQ) {
			response = Variables.readResp;
		} else if (req == Request.WRQ) {
			response = Variables.writeResp;
		}

		sendPacket = new DatagramPacket(response, response.length, clientIP, clientPort);

		System.out.println("[" + threadId + "]: " + "Server: Sending packet:");
		System.out.println("[" + threadId + "]: " + "To host: " + sendPacket.getAddress());
		System.out.println("[" + threadId + "]: " + "Destination host port: " + sendPacket.getPort());
		int length = sendPacket.getLength();
		System.out.println("[" + threadId + "]: " + "Length: " + length);
		System.out.println("[" + threadId + "]: " + "Containing: ");
		for (int j = 0; j < length; j++) {
			System.out.println("byte " + j + " " + response[j]);
		}

		try {
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.send(sendPacket);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("[" + threadId + "]: " + "Server: packet sent using port " + sendReceiveSocket.getLocalPort());
		System.out.println();
		
		while (true) {
			byte[] received = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(received, received.length);

			System.out.println("[" + threadId + "]: " + "Server: Waiting for packet.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if (req == Request.RRQ) {
				if (verifyACK(received)) {
					// TODO
					break;
				} else {
					// TODO
					// issue, request is not correct
					break;
				}
			} else if (req == Request.WRQ) {
				if (verifyDATA(received)) {
					writeToFile(filename, Arrays.copyOfRange(received, Variables.DATA.length, received.length));
					sendPacket = new DatagramPacket(Variables.ACK, Variables.ACK.length, clientIP, clientPort);
				} else {
					// TODO
					// issue, request is not correct
					break;
				}
			}
			
			System.out.println("[" + threadId + "]: " + "Server: Sending packet:");
			System.out.println("[" + threadId + "]: " + "To host: " + sendPacket.getAddress());
			System.out.println("[" + threadId + "]: " + "Destination host port: " + sendPacket.getPort());
			length = sendPacket.getLength();
			System.out.println("[" + threadId + "]: " + "Length: " + length);
			System.out.println("[" + threadId + "]: " + "Containing: ");
			for (int j = 0; j < length; j++) {
				System.out.println("byte " + j + " " + response[j]);
			}

			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("[" + threadId + "]: " + "Server: packet sent using port " + sendReceiveSocket.getLocalPort());
			System.out.println();
			
		}
		
		// We're finished with this socket, so close it.
		sendReceiveSocket.close();
	}

	public byte[] packageRead(String filename) throws IOException {
		// TODO
		Path path = Paths.get(Variables.SERVER_FILES_DIR + filename);
		byte[] data = Files.readAllBytes(path);
		return data;
	}

	public void writeToFile(String filename, byte[] fileContent) {
		Path path = Paths.get(Variables.SERVER_FILES_DIR + filename);
		try {
			Files.write(path, fileContent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	public boolean verifyACK(byte[] data) {
		for (int i = 0; i < Variables.ACK.length; i++) {
			if (Variables.ACK[i] != data[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean verifyDATA(byte[] data) {
		if (data.length <= Variables.DATA.length) { // no data in message
			return false;
		}
		for (int i = 0; i < Variables.DATA.length; i++) {
			if (Variables.DATA[i] != data[i]) {
				return false;
			}
		}
		return true;
	}
	
	public Request verifyRequest(byte[] data) {
		Request req; // READ, WRITE or ERROR
		int j = 0, k = 0;
		
		if (data[0] != 0) {
			return Request.ERROR; // bad
		} else if (data[1] == 1) {
			req = Request.RRQ; // could be read
		} else if (data[1] == 2) {
			req = Request.WRQ; // could be write
		} else {
			return Request.ERROR; // bad
		}
		
		if (req != Request.ERROR) { // check for filename
			// search for next all 0 byte
			for (j = 2; j < len; j++) {
				if (data[j] == 0) {
					break;
				}
			}
			if (j == len) {
				return Request.ERROR; // didn't find a 0 byte
			}
			if (j == 2) {
				return Request.ERROR; // filename is 0 bytes long
			}
		}

		if (req != Request.ERROR) { // check for mode
			// search for next all 0 byte
			for (k = j + 1; k < len; k++) {
				if (data[k] == 0) {
					break;
				}
			}
			if (k == len) {
				return Request.ERROR; // didn't find a 0 byte
			}
			if (k == j + 1) {
				return Request.ERROR; // mode is 0 bytes long
			}
		}

		if (k != len - 1) {
			return Request.ERROR; // other stuff at end of packet
		}
		
		return req;
	}

	public String pullFilename(byte[] data) {
		int j;
		for (j = 2; j < len; j++) {
			if (data[j] == 0)
				break;
		}
		return new String(data, 2, j - 2);
	}
	
}
