package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class DuplicatedThread extends ErrorThread {
	
	private DatagramSocket sendReceiveSocket = null;
	
	private int delay;
	private int position;
	private int packetType;
	private boolean duplicatedPacket;
	
	public DuplicatedThread(int packet, int position, int delay) {
		this.delay = delay;
		this.position = position;
		this.packetType = packet;
		this.duplicatedPacket = false;
	}
	
	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, serverIP, Variables.SERVER_PORT);

		Logger.logRequestPacketSending(sendPacket);
		
		// Send the datagram packet to the server via the
		// send/receive socket.
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
		
		if (isRequest(this.packetType, data)) {
			byte[] dupData = new byte[len];
			System.arraycopy(data, 0, dupData, 0, len);
			sendDuplicatePacket(new DatagramPacket(dupData, len, serverIP, serverPort));
		}
		
		while (true) {
			// Construct a DatagramPacket for receiving packets up
			// to 512 bytes long (the length of the byte array).
			if (receivePacket.getPort() == clientPort) {
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), serverIP,
						serverPort);
			} else {
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), clientIP,
						clientPort);
			}

			Logger.logPacketSending(sendPacket);

			if (!duplicatedPacket && isRequest(this.packetType, newData) && isPosition(position, newData)) {
				duplicatedPacket = true;
				sendDuplicatePacket(sendPacket);
			}
			
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
	
	private void sendDuplicatePacket(DatagramPacket sendPacket) {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					Logger.log("Sending duplicate packet.");
					if (packetType == 1 || packetType == 2) {
						Logger.logRequestPacketSending(sendPacket);
					} else {
						Logger.logPacketSending(sendPacket);						
					}
					sendReceiveSocket.send(sendPacket);
					Logger.log("Duplicate packet sent.");
					Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
					Logger.log("");
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					sendReceiveSocket.close();
					System.exit(1);
				}
			}
		});
		t.start();
	}
	
}
