package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class OpCodeThread extends ErrorThread {

	private int position;
	private int packetType;
	private int newOpCode;
	private boolean changeOpCode;
	// private boolean corruptOpcode;

	public OpCodeThread(int packet, int position, int newOpCode) {
		this.position = position;
		this.packetType = packet;
		this.newOpCode = newOpCode;
		this.changeOpCode = true;
		// this.corruptOpcode = false;
	}

	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);

		Logger.logRequestPacketSending(sendPacket);

		// Send the datagram packet to the server via the
		// send/receive socket.
		DatagramSocket sendReceiveSocket = null;
		try {

			// -------------------corrupt packet here--------------------------

			// check if its the right packet to corrupt
			if (isRequest(this.packetType, data)) {

				// corrupt opcode
				data[0] = 0;
				data[1] = (byte) newOpCode;

				// construct new packet with corrupted opcode
				sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);
				changeOpCode = false;
				Logger.log("Corrupted opcode.");
			}

			// ---------------------------------------------------------------

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

			try {
				// -------------------corrupt packet
				// here--------------------------

				// check if its the right packet to corrupt
				if ((isRequest(this.packetType, newData) && isPosition(position, newData) && changeOpCode)) {

					// corrupt opcode
					newData[0] = 0;
					newData[1] = (byte) newOpCode;

					// construct new packet with corrupted opcode
					if (receivePacket.getPort() == clientPort) {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), serverIP,
								serverPort);
					} else {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), clientIP,
								clientPort);
					}

					changeOpCode = false;
					Logger.log("Corrupted opcode.");
					// ---------------------------------------------------------------
				} else {

					// Construct a DatagramPacket for receiving packets up
					// to 512 bytes long (the length of the byte array).
					if (receivePacket.getPort() == clientPort) {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), serverIP,
								serverPort);
					} else {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), clientIP,
								clientPort);
					}

				}

				// log the packet being sent
				if (newData[1] == 1 || newData[1] == 2) {
					Logger.logRequestPacketSending(sendPacket);
				} else {
					Logger.logPacketSending(sendPacket);
				}

				// send the packet via sendReceive socket
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
}
