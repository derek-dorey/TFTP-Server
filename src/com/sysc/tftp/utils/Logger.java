package com.sysc.tftp.utils;

import java.net.DatagramPacket;
import java.util.Arrays;

public class Logger {

	/**
	 * Prints the contents of a request datagram packet
	 * 
	 * @param packet
	 *            The packet to print
	 */
	public static void logRequestPacketSending(DatagramPacket packet) {
		if (!Variables.VERBOSE) {
			return;
		}
		// to show which thread is doing what
		long threadId = Thread.currentThread().getId();
		logSend(packet, threadId);
		logRequest(packet.getData(), threadId);
		logContents(packet, threadId);
	}

	/**
	 * Prints the contents of a request datagram packet
	 * 
	 * @param packet
	 *            The packet to print
	 */
	public static void logRequestPacketReceived(DatagramPacket packet) {
		if (!Variables.VERBOSE) {
			return;
		}
		// to show which thread is doing what
		long threadId = Thread.currentThread().getId();
		logReceive(packet, threadId);
		logRequest(packet.getData(), threadId);
		logContents(packet, threadId);
	}

	/**
	 * Prints the contents of a datagram packet with the thread id
	 * 
	 * @param packet
	 *            The packet to print
	 */
	public static void logPacketReceived(DatagramPacket packet) {
		if (!Variables.VERBOSE) {
			return;
		}
		// to show which thread is doing what
		long threadId = Thread.currentThread().getId();
		logReceive(packet, threadId);
		logResponse(packet.getData(), threadId);
		logContents(packet, threadId);
	}

	/**
	 * Prints the contents of a datagram packet with the thread id
	 * 
	 * @param packet
	 *            The packet to print
	 */
	public static void logPacketSending(DatagramPacket packet) {
		if (!Variables.VERBOSE) {
			return;
		}
		// to show which thread is doing what
		long threadId = Thread.currentThread().getId();
		logSend(packet, threadId);
		logResponse(packet.getData(), threadId);
		logContents(packet, threadId);
	}

	/**
	 * Prints info with the thread id
	 * 
	 * @param info
	 */
	public static void log(String info) {
		if (!Variables.VERBOSE) {
			return;
		}
		// to show which thread is doing what
		long threadId = Thread.currentThread().getId();
		System.out.println("[" + threadId + "]: " + info);
	}

	/**
	 * Prints the contents of the datagram packet
	 * 
	 * @param packet
	 *            The packet to print
	 * @param threadId
	 *            The thread id of the current thread
	 */
	private static void logContents(DatagramPacket packet, long threadId) {
		int len = packet.getLength();
		System.out.println("[" + threadId + "]: " + "Length: " + len);
		System.out.println("[" + threadId + "]: " + "Containing: ");
		System.out.println("\t" + Arrays.toString(packet.getData()));
	}

	/**
	 * Log packet to send
	 * 
	 * @param packet
	 *            The packet to print
	 * @param threadId
	 *            The thread id of the current thread
	 */
	private static void logSend(DatagramPacket packet, long threadId) {
		System.out.println("[" + threadId + "]: " + "Sending packet:");
		System.out.println("[" + threadId + "]: " + "To IP: " + packet.getAddress());
		System.out.println("[" + threadId + "]: " + "To port: " + packet.getPort());
	}

	/**
	 * Log received packet
	 * 
	 * @param packet
	 *            The packet to print
	 * @param threadId
	 *            The thread id of the current thread
	 */
	private static void logReceive(DatagramPacket packet, long threadId) {
		System.out.println("[" + threadId + "]: " + "Received packet:");
		System.out.println("[" + threadId + "]: " + "From IP: " + packet.getAddress());
		System.out.println("[" + threadId + "]: " + "From port: " + packet.getPort());
	}

	/**
	 * Log the response type and block number
	 * 
	 * @param data
	 *            The data to search and print
	 * @param threadId
	 *            The thread id of the current thread
	 */
	private static void logResponse(byte[] data, long threadId) {
		// response type (i.e, DATA, ACK, ERROR)
		String type = null;
		switch (data[1]) {
		case 3:
			type = "DATA";
			break;
		case 4:
			type = "ACK";
			break;
		case 5:
			type = "ERROR";
			break;
		default:
			type = "Invalid";
		}

		System.out.println("[" + threadId + "]: " + "Response type: " + type);

		// block number
		int num = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
		System.out.println("[" + threadId + "]: " + "Block number: " + num);

	}

	/**
	 * Logs the type, filename and mode of a request
	 * 
	 * @param data
	 *            The data to search and print
	 * @param threadId
	 *            The thread id of the current thread
	 */
	private static void logRequest(byte[] data, long threadId) {
		// request type (i.e, RRQ, WRQ)
		String type = null;
		switch (data[1]) {
		case 1:
			type = "Read";
			break;
		case 2:
			type = "Write";
			break;
		default:
			type = "Invalid";
		}

		System.out.println("[" + threadId + "]: " + "Request type: " + type);

		// filename
		int j, k;
		for (j = 2; j < data.length; j++) {
			if (data[j] == 0)
				break;
		}
		String filename = new String(data, 2, j - 2);

		// mode
		for (k = j + 1; k < data.length; k++) {
			if (data[k] == 0) {
				break;
			}
		}
		String mode = new String(data, j, k - j);

		System.out.println("[" + threadId + "]: " + "Filename: " + filename);
		System.out.println("[" + threadId + "]: " + "Mode: " + mode);
	}

}
