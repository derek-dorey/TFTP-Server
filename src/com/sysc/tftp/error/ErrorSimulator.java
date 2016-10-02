package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class ErrorSimulator {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket receiveSocket, sendSocket, sendReceiveSocket;

	public ErrorSimulator() {
		try {
			// Construct a datagram socket and bind it to port 23
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(Variables.ERROR_PORT);
			// Construct a datagram socket and bind it to any available
			// port on the local host machine. This socket will be used to
			// send and receive UDP Datagram packets from the server.
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void passOnTFTP() {

		byte[] data;

		int clientPort = 0;

		for (;;) { // loop forever
			// Construct a DatagramPacket for receiving packets
			// to 512 bytes long (the length of the byte array).

			data = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(data, data.length);

			Logger.log("Simulator: Waiting for packet.");
			// Block until a datagram packet is received from receiveSocket.
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			clientPort = receivePacket.getPort();

			Logger.logPacketReceived(receivePacket);

			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					receivePacket.getAddress(), Variables.SERVER_PORT);

			Logger.logPacketSending(sendPacket);

			// Send the datagram packet to the server via the send/receive
			// socket.
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// Construct a DatagramPacket for receiving packets up
			// to 512 bytes long (the length of the byte array).

			data = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(data, data.length);

			Logger.log("Simulator: Waiting for packet.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Logger.logPacketReceived(receivePacket);

			sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort);

			Logger.logPacketSending(sendPacket);

			// Send the datagram packet to the client via a new socket.

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

			Logger.log("Simulator: packet sent using port " + sendSocket.getLocalPort());
			Logger.log("");

			// We're finished with this socket, so close it.
			sendSocket.close();
		}

	}

	public static void main(String args[]) {
		if (Arrays.asList(args).contains(Variables.VERBOSE_FLAG)) {
			Variables.VERBOSE = true;
		}

		ErrorSimulator es = new ErrorSimulator();
		es.passOnTFTP();
	}
}
