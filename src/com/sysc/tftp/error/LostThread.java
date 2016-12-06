package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class LostThread extends ErrorThread {
	
	private int position;
	private int packetType;
	private boolean lostPacket;
	
	public LostThread(int packet, int position) {
		this.position = position;
		this.packetType = packet;
		this.lostPacket = false;
	}
	
	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);

		Logger.logRequestPacketSending(sendPacket);

		// Send the datagram packet to the server via the
		// send/receive socket.
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
			// Block until a datagram is received via sendReceiveSocket.
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
			if (receivePacket.getPort() == clientPort) {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), serverIP,
						serverPort);
			} else {
				sendPacket = new DatagramPacket(data, receivePacket.getLength(), clientIP,
						clientPort);
			}

			if (!lostPacket && isRequest(this.packetType, data) && isPosition(position, data)) {
				Logger.log("Simulated packet lose");
				lostPacket = true;
			} else {
				Logger.logPacketSending(sendPacket);
				// Send the datagram packet to the client via a new socket.
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
				Logger.log("");				
			}
			
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
		}
	}
}
