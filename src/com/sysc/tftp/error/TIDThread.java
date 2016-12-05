package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class TIDThread extends ErrorThread {

	private int position;
	private int packetType;
	private boolean changedTID;

	private DatagramSocket sendReceiveSocket = null;

	public TIDThread(int packet, int position) {
		this.position = position;
		this.packetType = packet;
		this.changedTID = false;
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
			Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
			Logger.log("");
		} catch (IOException e) {
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}

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
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), serverIP,
						serverPort);
			} else {
				sendPacket = new DatagramPacket(newData, receivePacket.getLength(), clientIP,
						clientPort);
			}


			// Send the datagram packet to the client via a new socket.
			try {
				if (!changedTID && (isRequest(this.packetType, newData) && isPosition(position, newData))) {
					changedTID = true;
					sendReceiveWithDifferentSocket(sendPacket);
				} else {
					Logger.logPacketSending(sendPacket);
					sendReceiveSocket.send(sendPacket);
					Logger.log("Simulator: packet sent using port " + sendReceiveSocket.getLocalPort());
					Logger.log("");
				}

				newData = new byte[Variables.MAX_PACKET_SIZE];
				receivePacket = new DatagramPacket(newData, newData.length);

				Logger.log("Simulator: Waiting for packet.");

				sendReceiveSocket.receive(receivePacket);
				Logger.logPacketReceived(receivePacket);

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private void sendReceiveWithDifferentSocket(DatagramPacket sendPacket) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket differentSocket = null;
				try {
					differentSocket = new DatagramSocket(sendReceiveSocket.getPort() + 1);

					Logger.log("Sending packet with different TID.");
					Logger.logPacketSending(sendPacket);
					differentSocket.send(sendPacket);

					Logger.log("Sent packet with different TID.");
					Logger.log("Simulator: packet sent using port " + differentSocket.getLocalPort());
					Logger.log("");

					byte[] newData = new byte[Variables.MAX_PACKET_SIZE];
					DatagramPacket receivePacket = new DatagramPacket(newData, newData.length);

					Logger.log("Simulator: Waiting for packet.");

					differentSocket.receive(receivePacket);
					Logger.logPacketReceived(receivePacket);

				} catch (IOException e) {
					e.printStackTrace();
					differentSocket.close();
					System.exit(1);
				}

			}
		});
		t.start();
	}

}
