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
	
	private DatagramPacket dupPacket;
	
	public DuplicatedThread(int packet, int position, int delay) {
		this.delay = delay;
		this.position = position;
		this.packetType = packet;
		this.duplicatedPacket = false;
	}
	
	@Override
	public void run() {
		DatagramPacket sendPacket = new DatagramPacket(data, len, clientIP, Variables.SERVER_PORT);

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
			dupPacket = new DatagramPacket(dupData, len, clientIP, serverPort);
			sendDuplicatePacket();
		}
		
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

			if (!duplicatedPacket && isRequest(this.packetType, newData) && isPosition(position, newData)) {
				byte[] dupData = new byte[receivePacket.getLength()];
				System.arraycopy(newData, 0, dupData, 0, receivePacket.getLength());
				dupPacket = new DatagramPacket(dupData, receivePacket.getLength(), receivePacket.getAddress(),
						clientPort);
				duplicatedPacket = true;
				sendDuplicatePacket();
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
	
	private void sendDuplicatePacket() {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					Logger.log("Sending duplicate packet.");
					if (packetType == 1 || packetType == 2) {
						Logger.logRequestPacketSending(dupPacket);
					} else {
						Logger.logPacketSending(dupPacket);						
					}
					sendReceiveSocket.send(dupPacket);
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
