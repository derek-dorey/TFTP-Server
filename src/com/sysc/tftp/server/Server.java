package com.sysc.tftp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server implements Runnable {

	// types of requests we can receive
	public static enum Request {
		READ, WRITE, ERROR
	};

	// responses for valid requests
	public static final byte[] readResp = { 0, 3, 0, 1 };
	public static final byte[] writeResp = { 0, 4, 0, 0 };

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private Thread thread = null;

	public Server() {
		try {
			// Construct a datagram socket and bind it to port 69
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(69);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				long threadId = Thread.currentThread().getId(); // for printing, to show which thread is doing what
				byte[] data = new byte[100];
				receivePacket = new DatagramPacket(data, data.length);

				System.out.println("[" + threadId + "]: " + "Server: Waiting for packet.");
				// Block until a datagram packet is received from receiveSocket.
				try {
					receiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// Process the received datagram.
				System.out.println("[" + threadId + "]: " + "Server: Packet received:");
				System.out.println("[" + threadId + "]: " + "From host: " + receivePacket.getAddress());
				System.out.println("[" + threadId + "]: " + "Host port: " + receivePacket.getPort());
				int len = receivePacket.getLength();
				System.out.println("[" + threadId + "]: " + "Length: " + len);
				System.out.println("[" + threadId + "]: " + "Containing: ");

				new Thread(new ClientConnection(data, len, receivePacket.getAddress(), receivePacket.getPort())).start();;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	
	/**
	 * 
	 * @author lexibrown
	 * Made client connection class because when project grows file transfer
	 * will be more code and we shouldn't throw it all in the server file and
	 * make this long file
	 * 
	 * So, in the future, this class can go in it's own file
	 *
	 */
	private class ClientConnection implements Runnable {

		private Thread thread = null;
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
			long threadId = Thread.currentThread().getId(); // for printing, to show which thread is doing what
			byte[] response = new byte[4];

			Request req; // READ, WRITE or ERROR

			String filename, mode;
			int j = 0, k = 0;

			// If it's a read, send back DATA (03) block 1
			// If it's a write, send back ACK (04) block 0
			// Otherwise, ignore it
			if (data[0] != 0)
				req = Request.ERROR; // bad
			else if (data[1] == 1)
				req = Request.READ; // could be read
			else if (data[1] == 2)
				req = Request.WRITE; // could be write
			else
				req = Request.ERROR; // bad

			if (req != Request.ERROR) { // check for filename
				// search for next all 0 byte
				for (j = 2; j < len; j++) {
					if (data[j] == 0)
						break;
				}
				if (j == len)
					req = Request.ERROR; // didn't find a 0 byte
				if (j == 2)
					req = Request.ERROR; // filename is 0 bytes long
				// otherwise, extract filename
				filename = new String(data, 2, j - 2);
			}

			if (req != Request.ERROR) { // check for mode
				// search for next all 0 byte
				for (k = j + 1; k < len; k++) {
					if (data[k] == 0)
						break;
				}
				if (k == len)
					req = Request.ERROR; // didn't find a 0 byte
				if (k == j + 1)
					req = Request.ERROR; // mode is 0 bytes long
				mode = new String(data, j, k - j - 1);
			}

			if (k != len - 1)
				req = Request.ERROR; // other stuff at end of packet

			// Create a response.
			if (req == Request.READ) { // for Read it's 0301
				response = readResp;
			} else if (req == Request.WRITE) { // for Write it's 0400
				response = writeResp;
			} else { // it was invalid, just quit
				// TODO
			}

			DatagramPacket sendPacket = new DatagramPacket(response, response.length, clientIP, clientPort);

			System.out.println("[" + threadId + "]: " + "Server: Sending packet:");
			System.out.println("[" + threadId + "]: " + "To host: " + sendPacket.getAddress());
			System.out.println("[" + threadId + "]: " + "Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("[" + threadId + "]: " + "Length: " + len);
			System.out.println("[" + threadId + "]: " + "Containing: ");
			for (j = 0; j < len; j++) {
				System.out.println("byte " + j + " " + response[j]);
			}

			// Send the datagram packet to the client via a new socket.

			DatagramSocket sendSocket = null;
			try {
				// Construct a new datagram socket and bind it to any port
				// on the local host machine. This socket will be used to
				// send UDP Datagram packets.
				sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}

			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("[" + threadId + "]: " + "Server: packet sent using port " + sendSocket.getLocalPort());
			System.out.println();

			// We're finished with this socket, so close it.
			sendSocket.close();
		}

		public void start() {
			if (thread == null) {
				thread = new Thread(this);
				thread.start();
			}
		}

	}

	public static void main(String args[]) throws Exception {
		Server s = new Server();
		s.start();
	}

}
