package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class ModeThread extends ErrorThread {
	
	private String mode;
	
	public ModeThread(String mode) {
		this.mode = mode;
	}
	
	@Override
	public void run() {
		changeMode();
		Logger.log("Mode changed.");
		
		DatagramPacket sendPacket = new DatagramPacket(data, len, clientIP, Variables.SERVER_PORT);

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
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), receivePacket.getAddress(),
						serverPort);
			} else {
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);
			}

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
	
	public void changeMode() {
		int j;
		for (j = 2; j < data.length; j++) {
			if (data[j] == 0)
				break;
		}
		byte[] newMessage = new byte[j + this.mode.length() + 2];
		
		System.arraycopy(data, 0, newMessage, 0, j);

		byte[] newMode = this.mode.getBytes();

		System.arraycopy(newMode, 0, newMessage, j + 1, newMode.length);
		newMessage[newMessage.length - 1] = 0;
		
		data = newMessage;
		len = newMessage.length;
	}
	
}