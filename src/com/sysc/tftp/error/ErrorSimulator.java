package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

import com.sysc.tftp.utils.Logger;
import com.sysc.tftp.utils.Variables;

public class ErrorSimulator implements Runnable {

	// UDP datagram packets and sockets used to send / receive
	private DatagramPacket receivePacket;
	private DatagramSocket receiveSocket;

	private Thread thread = null; // the thread the listener sits ons
	private Thread toExit = null; // thread that closes all threads on shutdown

	private boolean running = true;

	private List<Thread> threads = new ArrayList<Thread>(); // list of threads
	// next thread to run
	private Queue<ErrorThread> nextThread = new LinkedList<ErrorThread>();

	public ErrorSimulator() {
		try {
			// Construct a datagram socket and bind it to port 23
			// on the local host machine. This socket will be used to
			// receive UDP Datagram packets from clients.
			receiveSocket = new DatagramSocket(Variables.ERROR_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		while (running) {
			// Construct a DatagramPacket for receiving packets
			// to 512 bytes long (the length of the byte array).

			byte[] data = new byte[Variables.MAX_PACKET_SIZE];
			receivePacket = new DatagramPacket(data, data.length);

			Logger.log("Simulator: Waiting for packet.");
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

			Logger.logRequestPacketReceived(receivePacket);

			Thread t = null;
			if (nextThread.isEmpty()) {
				t = new Thread(new NormalThread(receivePacket.getData(), receivePacket.getLength(),
						receivePacket.getAddress(), receivePacket.getPort()));
			} else {
				ErrorThread error = nextThread.poll();
				error.setInfo(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(),
						receivePacket.getPort());
				t = new Thread(error);
			}
			threads.add(t);
			t.start();
		}
	}

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
					while (true) {
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
						} else if (s != null && !s.isEmpty()) {
							handleSim(s);
						}
					}
				}
			});
			toExit.start();
		}
	}

	/**
	 * Waits for all error threads to finish before closing them
	 */
	public void closeThreads() {
		Logger.log("Closing connections...");
		nextThread.clear();
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

	public static void main(String args[]) {
		ErrorSimulator es = new ErrorSimulator();
		es.start();
	}
	
	/**
	 * Handles commands with parameters
	 * @param s, user command
	 */
	public void handleSim(String s) {
		List<String> requests = Arrays.asList("rrq", "wrq", "data", "ack");
		String input = s.toLowerCase().trim();
		try {
			String params[] = input.split(" ");
			if (!"delay".equals(params[0]) && !"dup".equals(params[0]) && !"lose".equals(params[0])) {
				return;
			} else if (!requests.contains(params[1])) {
				System.out.println("Invalid request type");
				return;
			} else if (("delay".equals(params[0]) || "dup".equals(params[0])) && params.length < 4) {
				System.out.println("Invalid parameters");
				return;
			} else if ("lose".equals(params[0]) && params.length < 3) {
				System.out.println("Invalid parameters");
				return;
			}

			int position = Integer.parseInt(params[2]);
			if (position <= 0) {
				System.out.println("Cannot have negative numbers or zero");
				return;
			}

			int requestType = requests.indexOf(params[1]) + 1;
			if ("lose".equals(params[0])) {
				if (requestType == 1 || requestType == 2) {
					System.out.println("No point losing the init request");
					return;
				}
				addToQueue(1, requestType, position, 0);
			} else {
				int delay = Integer.parseInt(params[3]);
				if (delay < 0) {
					System.out.println("Cannot have negative numbers");
					return;
				}

				if ("delay".equals(params[0])) {
					if (requestType == 1 || requestType == 2) {
						System.out.println("No point in delaying the init request");
						return;
					}
					addToQueue(2, requestType, position, delay);
				} else if ("dup".equals(params[0])) {
					addToQueue(3, requestType, position, delay);
				}
			}
		} catch (Exception e) {
			System.out.println("Invalid command");
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Adds a error simulating thread to the queue
	 * @param operation, lose(1), delay(2) or duplicate(3)
	 * @param packet, rrw, wrq, ack or data
	 * @param position, position of packet
	 * @param delay, amount to delay in ms
	 */
	public void addToQueue(int operation, int packet, int position, int delay) {
		ErrorThread error = null;
		switch (operation) {
		case 1: // lost packet
			error = new LostThread(packet, position);
			break;
		case 2: // delay packet
			error = new DelayThread(packet, position);
			break;
		case 3: // duplication packet
			error = new DuplicatedThread(packet, position, delay);
			break;

		default:
			System.out.println("Unknown thread.");
			return;
		}
		nextThread.add(error);
	}

	/**
	 * Prints help for console commands
	 */
	public void printHelp() {
		System.out.println("TFTP Error Simulator");
		System.out.println("<r> must be 'ack','data', 'wrq'. or 'rrq'");
		System.out.println("<p> position of packet");
		System.out.println("<d> delay in millisecounds");
		System.out.println("The server and client timeout is " + Variables.packetTimeout + "ms");
		System.out.println("\tCommands:");
		System.out.println("\thelp					Prints this message");
		System.out.println("\tverbose <on/off>			Turns verbose mode on or off");
		System.out.println("\tquit					Exits the simulator");
		System.out.println("\tdelay	<r> <p> <d>			Delays the specified packet by a number of ms");
		System.out.println("\tdup	<r> <p> <d>			Sends a duplicate of the specified packet");
		System.out.println("\tlose	<r> <p>				Loses the specified packet");
	}

}
