package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class TIDThread extends ErrorThread {
	
	private int position;
	private int packetType;
	private boolean changedTID;
	
	public TIDThread(int packet, int position) {
		this.position = position;
		this.packetType = packet;
		this.changedTID = false;
	}
	
	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, clientIP, Variables.SERVER_PORT);

		Logger.logRequestPacketSending(sendPacket);
				
		// Send the datagram packet to the server via the
		// send/receive socket.
		DatagramSocket sendReceiveSocket = null;
		DatagramSocket differentSocket = null;
		try {
			sendReceiveSocket = new DatagramSocket();
			differentSocket = new DatagramSocket(sendReceiveSocket.getPort() + 1);
			if (isRequest(this.packetType, data)) {
				changedTID = true;
				Logger.log("Sending packet with different TID.");
				differentSocket.send(sendPacket);
				Logger.log("Sent packet with different TID.");
				Logger.log("Simulator: packet sent using port " + differentSocket.getLocalPort());
				Logger.log("");
			} else {
				sendReceiveSocket.send(sendPacket);
				Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
				Logger.log("");
			}			
		} catch (IOException e) {
			e.printStackTrace();
			sendReceiveSocket.close();
			differentSocket.close();
			System.exit(1);
		}

		byte[] newData = new byte[Variables.MAX_PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(newData, newData.length);

		Logger.log("Simulator: Waiting for packet.");
		try {
			// Block until a datagram is received via sendReceiveSocket.
			if (changedTID) { 
				changedTID = false; // only for rrq or wrq TID changes
				differentSocket.receive(receivePacket);
			} else {
				sendReceiveSocket.receive(receivePacket);				
			}
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
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);
			} else {
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);
			}

			Logger.logPacketSending(sendPacket);

			// Send the datagram packet to the client via a new socket.
			try {
				if (changedTID || (isRequest(this.packetType, newData) && isPosition(position, newData))) {
					changedTID = true;
					Logger.log("Sending packet with different TID.");
					differentSocket.send(sendPacket);
					Logger.log("Sent packet with different TID.");
					Logger.log("Simulator: packet sent using port " + differentSocket.getLocalPort());
					Logger.log("");
				} else {
					sendReceiveSocket.send(sendPacket);
					Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
					Logger.log("");
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			newData = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(newData, newData.length);

			Logger.log("Simulator: Waiting for packet.");
			try {
				// Block until a datagram is received via sendReceiveSocket.
				if (changedTID) { 
					differentSocket.receive(receivePacket);
				} else {
					sendReceiveSocket.receive(receivePacket);				
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Logger.logPacketReceived(receivePacket);
		}
	}	
}
