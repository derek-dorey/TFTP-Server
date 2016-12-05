package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class DelayThread extends ErrorThread {

	private DatagramSocket sendReceiveSocket = null;
	
	private int position;
	private int packetType;
	private int delay;
	private boolean delayedPacket;
	
	public DelayThread(int packet, int position, int delay) {
		this.packetType = packet;
		this.position = position;
		this.delay = delay;
		this.delayedPacket = false;
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
			
			if (!delayedPacket && isRequest(this.packetType, data) && isPosition(position, data)) {
				delayedPacket = true;
				delayPacket(sendPacket);
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
	
	private void delayPacket(DatagramPacket sendPacket) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					Logger.log("Sending Delayed packet.");
					Logger.logPacketSending(sendPacket);
					sendReceiveSocket.send(sendPacket);
					Logger.log("Delayed packet sent.");
					Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
					Logger.log("");
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
					sendReceiveSocket.close();
					System.exit(1);
				}
			}
		});
		t.start();
	}

}
