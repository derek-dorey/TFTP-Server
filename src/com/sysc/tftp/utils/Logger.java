package com.sysc.tftp.utils;

import java.net.DatagramPacket;

public class Logger {

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

		System.out.println("[" + threadId + "]: " + "Packet received:");
		System.out.println("[" + threadId + "]: " + "From IP: " + packet.getAddress());
		System.out.println("[" + threadId + "]: " + "From port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("[" + threadId + "]: " + "Length: " + len);
		System.out.println("[" + threadId + "]: " + "Containing: ");
		for (int j = 0; j < len; j++) {
			System.out.println("\tbyte " + j + " " + packet.getData()[j]);
		}
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

		System.out.println("[" + threadId + "]: " + "Sending packet:");
		System.out.println("[" + threadId + "]: " + "To IP: " + packet.getAddress());
		System.out.println("[" + threadId + "]: " + "To port: " + packet.getPort());
		int len = packet.getLength();
		System.out.println("[" + threadId + "]: " + "Length: " + len);
		System.out.println("[" + threadId + "]: " + "Containing: ");
		for (int j = 0; j < len; j++) {
			System.out.println("\tbyte " + j + " " + packet.getData()[j]);
		}
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

}
