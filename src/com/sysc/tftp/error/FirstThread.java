package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class FirstThread extends ErrorThread {

	private int position;
	private int packetType;
	private boolean removedFirst;

	public FirstThread(int packet, int position) {
		this.position = position;
		this.packetType = packet;
		this.removedFirst = false;
	}

	@Override
	public void run() {
		if (isRequest(this.packetType, data)) {
			data = removeFirst(data, len);
			len = data.length;
			removedFirst = true;
			Logger.log("First byte removed.");
		}

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
		Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
		Logger.log("");

		byte[] newData = new byte[Variables.MAX_PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(newData, newData.length);
		len = receivePacket.getLength();

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
			len = receivePacket.getLength();

			if (!removedFirst && (isRequest(this.packetType, newData) && isPosition(position, newData))) {
				newData = removeFirst(newData, len);
				len = newData.length;
				removedFirst = true;
				Logger.log("First byte removed.");
			}
			// Construct a DatagramPacket for receiving packets up
			// to 512 bytes long (the length of the byte array).
			if (receivePacket.getPort() == clientPort) {
				sendPacket = new DatagramPacket(newData, len, serverIP, serverPort);
			} else {
				sendPacket = new DatagramPacket(newData, len, clientIP, clientPort);
			}

			try {
				Logger.logPacketSending(sendPacket);
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
			Logger.log("");

			newData = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(newData, newData.length);

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

	public byte[] removeFirst(byte[] data, int len) {
		byte[] newMessage = new byte[len - 1];

		System.arraycopy(data, 1, newMessage, 0, len - 1);

		data = newMessage;
		len = newMessage.length;
		return newMessage;
	}

}
