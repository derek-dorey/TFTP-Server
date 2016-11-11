package com.sysc.tftp.error;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
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
	private Queue<ErrorThread> nextThread = new LinkedList<ErrorThread>(); // next
																			// thread
																			// to
																			// run

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
				error.setInfo(receivePacket.getData(), receivePacket.getLength(),
						receivePacket.getAddress(), receivePacket.getPort());
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
					while (true) {
						System.out.println("Type '!quit' to shutdown error simulator");
						String s = scan.nextLine();
						if ("!quit".equals(s)) {
							scan.close();
							closeThreads();
							break;
						} else if ("error".equals(s)) {
							addErrorToQueue(scan);
						} else {
							switch (s.toLowerCase().trim()) {
							case Variables.SET_VERBOSE_ON:
								Variables.VERBOSE = true;
								System.out.println("\nVerbose: [ON]\n");
								break;
							case Variables.SET_VERBOSE_OFF:
								Variables.VERBOSE = false;
								System.out.println("\nVerbose: [OFF]\n");
								break;
							}
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

	public void addErrorToQueue(Scanner scan) {
		while (true) {
			System.out.println();
			System.out.println("Select next operation to use: ");
			System.out.println("(1) Lost a packet");
			System.out.println("(2) Delay packet");
			System.out.println("(3) Duplicate packet");
			System.out.println("(0) Go Back");
			String s = scan.nextLine();
			try {
				int operation = Integer.parseInt(s);
				if (operation == 0) {
					return;
				} else if (operation > 3 || operation < 0) {
					System.out.println("Invalid input.");
				} else {
					System.out.println();
					System.out.println("Select which packet type to target: ");
					System.out.println("(1) RRQ");
					System.out.println("(2) WRQ");
					System.out.println("(3) DATA");
					System.out.println("(4) ACK");

					s = scan.nextLine();
					int packet = Integer.parseInt(s);
					if (packet > 4 || packet < 0) {
						System.out.println("Invalid input.");
					} else {
						if (packet == 3 || packet == 4) {
							System.out.println();
							System.out.println("Which packet?");

							s = scan.nextLine();
							int position = Integer.parseInt(s);
							if (position < 1) {
								System.out.println("Invalid input.");
							} else if (operation == 3) {
								System.out.println();
								System.out.println("How much delay between duplicated packets?");

								s = scan.nextLine();
								int delay = Integer.parseInt(s);
								if (delay < 0) {
									System.out.println("Invalid input.");
								} else {
									addToQueue(operation, packet, position, delay);
									return;
								}
							} else {
								addToQueue(operation, packet, position, 0);
								return;
							}
						} else if (operation == 3) {
							while (true) {
								System.out.println();
								System.out.println("How much delay between duplicated packets?");

								s = scan.nextLine();
								int delay = Integer.parseInt(s);
								if (delay < 0) {
									System.out.println("Invalid input.");
								} else {
									addToQueue(operation, packet, 1, delay);
									return;
								}
							}
						} else {
							addToQueue(operation, packet, 1, 0);
							return;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("Invalid input.");
			}
		}
	}
	
	public void addToQueue(int operation, int packet, int position, int delay) {
		ErrorThread error = null;
		switch(operation) {
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

}
