package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class ErrorSimulator implements Runnable {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private Thread thread = null; // the thread the listener sits ons

	public ErrorSimulator() {
		try {
			// Construct a datagram socket and bind it to port 23
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(Variables.ERROR_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		for (;;) { // loop forever
			// Construct a DatagramPacket for receiving packets
			// to 512 bytes long (the length of the byte array).

			byte[] data = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(data, data.length);

			Logger.log("Simulator: Waiting for packet.");
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Logger.logPacketReceived(receivePacket);
			Thread t = new Thread(new HostThread(receivePacket.getData(), receivePacket.getLength(),
					receivePacket.getAddress(), receivePacket.getPort()));
			t.start();
		}

	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public static void main(String args[]) {
		if (Arrays.asList(args).contains(Variables.VERBOSE_FLAG)) {
			Variables.VERBOSE = true;
		}

		ErrorSimulator es = new ErrorSimulator();
		es.start();
	}

	private class HostThread implements Runnable {

		private byte[] data = null; // holds the original request

		// client information: port, IP, length of data
		private int len = 0, clientPort = 0;
		private InetAddress clientIP = null;

		public HostThread(byte[] data, int len, InetAddress ip, int port) {
			this.data = data;
			this.len = len;
			this.clientIP = ip;
			this.clientPort = port;
		}

		@Override
		public void run() {
			DatagramPacket sendPacket = new DatagramPacket(data, len, clientIP, Variables.SERVER_PORT);

			Logger.logPacketSending(sendPacket);

			// Send the datagram packet to the server via the
			// send/receive
			// socket.
			DatagramSocket sendReceiveSocket = null;
			try {
				sendReceiveSocket = new DatagramSocket();
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				sendReceiveSocket.close();
				System.exit(1);
			}

			data = new byte[Variables.MAX_PACKET_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(data, data.length);

			Logger.log("Simulator: Waiting for packet.");
			try {
				// Block until a datagram is received via
				// sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Logger.logPacketReceived(receivePacket);
			int serverPort = receivePacket.getPort();

			while (true) {
				// Construct a DatagramPacket for receiving packets up
				// to 512 bytes long (the length of the byte array).

				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);

				Logger.logPacketSending(sendPacket);

				// Send the datagram packet to the client via a new
				// socket.
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
				Logger.log("");

				data = new byte[Variables.MAX_PACKET_SIZE];
				receivePacket = new DatagramPacket(data, data.length);

				Logger.log("Simulator: Waiting for packet.");
				try {
					// Block until a datagram is received via
					// sendReceiveSocket.
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				Logger.logPacketReceived(receivePacket);

				sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);

				Logger.logPacketSending(sendPacket);

				// Send the datagram packet to the client via a new
				// socket.
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
				Logger.log("");

				data = new byte[Variables.MAX_PACKET_SIZE];
				receivePacket = new DatagramPacket(data, data.length);

				Logger.log("Simulator: Waiting for packet.");
				try {
					// Block until a datagram is received via
					// sendReceiveSocket.
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				Logger.logPacketReceived(receivePacket);
			}
		}
	}
}
