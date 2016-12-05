package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.BlockUtil;
import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class BlockThread extends ErrorThread {

	private int position;
	private int newPosition;
	private int packetType;
	private boolean changeBlock;

	public BlockThread(int packet, int position, int newPosition) {
		this.position = position;
		this.packetType = packet;
		this.newPosition = newPosition;
		this.changeBlock = true;
	}

	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);

		Logger.logRequestPacketSending(sendPacket);

		// Send the datagram packet to the server via the
		// send/receive socket.
		DatagramSocket sendReceiveSocket = null;
		try {

			// -------------------Change Block# here (not sure if this is
			// needed, as request will never have
			// block#)--------------------------

			// check if its the right packet to change block#
			if (isRequest(this.packetType, data)) {
				// change block#
				byte[] block = BlockUtil.intToByte(newPosition);
				data[2] = block[0];
				data[3] = block[1];

				// construct new packet with changed block#
				sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);
				changeBlock = false;
				Logger.log("Changed block number.");
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
				// -------------------Change Block#
				// here--------------------------

				// check if its the right packet to change block#
				if ((isRequest(this.packetType, newData) && isPosition(position, newData) && changeBlock)) {

					byte[] block = BlockUtil.intToByte(newPosition);
					newData[2] = block[0];
					newData[3] = block[1];

					// construct new packet with corrupted opcode
					if (receivePacket.getPort() == clientPort) {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), serverIP,
								serverPort);
					} else {
						sendPacket = new DatagramPacket(newData, receivePacket.getLength(), clientIP,
								clientPort);
					}

					changeBlock = false;
					Logger.log("Changed block number.");
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
