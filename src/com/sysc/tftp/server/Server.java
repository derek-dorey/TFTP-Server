package com.sysc.tftp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.sysc.tftp.utils.Variables;

public class Server implements Runnable {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private Thread thread = null;

	public Server() {
		try {
			// Construct a datagram socket and bind it to port 69
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(Variables.SERVER_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				long threadId = Thread.currentThread().getId(); // for printing,
																// to show which
																// thread is
																// doing what
				byte[] data = new byte[Variables.MAX_PACKET_SIZE];
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

				new Thread(new ClientConnection(data, len, receivePacket.getAddress(), receivePacket.getPort()))
						.start();

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

	public static void main(String args[]) throws Exception {
		Server s = new Server();
		s.start();
	}

}
