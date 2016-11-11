package com.sysc.tftp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class Server implements Runnable {

	// UDP datagram packet and socket used to receive
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private Thread thread = null; // the thread the listener sits on
	private Thread toExit = null; // thread that closes all threads on shutdown

	private boolean running = true;

	private List<Thread> threads = new ArrayList<Thread>(); // list of threads

	public Server() {
		try {
			// Construct a datagram socket and bind it to port 69
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(Variables.SERVER_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Waits for client to connect and dispatches thread to handle client
	 * request
	 */
	@Override
	public void run() {
		try {
			while (running) {
				byte[] data = new byte[Variables.MAX_PACKET_SIZE];
				receivePacket = new DatagramPacket(data, data.length);

				Logger.log("Server: Waiting for packet.");

				// Block until a datagram packet is received from receiveSocket.
				try {
					receiveSocket.receive(receivePacket);
				} catch (SocketException se) {
					running = false;
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}

				// Process the received datagram.
				Logger.logRequestPacketReceived(receivePacket);

				Thread t = new Thread(new ClientConnection(receivePacket.getData(), receivePacket.getLength(),
						receivePacket.getAddress(), receivePacket.getPort()));
				threads.add(t);
				t.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.log("Exiting...");
	}

	/**
	 * Starts the server thread listening for connections and starts another
	 * thread to wait for console input to shutdown the server
	 */
	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
		if (toExit == null) {
			toExit = new Thread(new Runnable() {
				@Override
				public void run() {
					Scanner scan = new Scanner(System.in);
					printHelp();
					do {
						String s = scan.nextLine();
						if ("quit".equals(s)) {
							scan.close();
							closeThreads();
							break;
						} else if ("help".equals(s)) {
							printHelp();
						} else if (s.toLowerCase().contains("verbose")) {
							try {
								String setting = s.split(" ")[1].toLowerCase().trim();
								if ("on".equals(setting)) {
									Variables.VERBOSE = true;
									System.out.println("\nVerbose: [ON]\n");
								} else if ("off".equals(setting)) {
									Variables.VERBOSE = false;
									System.out.println("\nVerbose: [OFF]\n");
								}
							} catch (Exception e) {
								continue;
							}
						}
					} while (scan.hasNext());
				}
			});
			toExit.start();
		}
	}

	/**
	 * Waits for all client threads to finish before closing them
	 */
	public void closeThreads() {
		Logger.log("Closing connections...");
		thread.interrupt();
		running = false;
		for (int i = 0; i < threads.size(); i++) {
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				Logger.log("Failed to join thread");
				e.printStackTrace();
			}
		}
		Logger.log("Connections closed.");
		receiveSocket.close();
	}
	
	/**
	 * Prints help for console commands
	 */
	public void printHelp() {
		System.out.println("TFTP Server");
		System.out.println("\tCommands:");
		System.out.println("\thelp					Prints this message");
		System.out.println("\tverbose <on/off>			Turns verbose mode on or off");
		System.out.println("\tquit					Exits the server");
	}

	public static void main(String args[]) throws Exception {
		Server s = new Server();
		s.start();
	}

}
