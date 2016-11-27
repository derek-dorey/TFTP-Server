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
	 * 
	 * @param s,
	 *            user command
	 */
	public void handleSim(String s) {
		String input = s.toLowerCase().trim();
		try {
			switch (input.split(" ")[0]) {
			case "delay":
				handle(input);
				break;
			case "dup":
				handle(input);
				break;
			case "lose":
				handle(input);
				break;
			case "mode":
				handleMode(input);
				break;
			case "file":
				handleFile(input);
				break;
			case "first":
				handleFirst(input);
				break;
			case "last":
				handleLast(input);
				break;
			case "sep":
				handleSep(input);
				break;

			case "block":
				handleBlock(input);
				break;
			case "opcode":
				handleOpCode(input);
				break;
			case "tid":
				handleTID(input);
				break;
			default:
				System.out.println("Unsuported command.");
				break;
			}
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleMode(String s) {
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 2) {
				System.out.println("Invalid parameters");
				return;
			}
			ErrorThread error = new ModeThread(params[1]);
			System.out.println("Added ModeThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleFile(String s) {
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 2) {
				System.out.println("Invalid parameters");
				return;
			}
			ErrorThread error = new FileThread(params[1]);
			System.out.println("Added FileThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleFirst(String s) {
		List<String> requests = Arrays.asList("rrq", "wrq", "data", "ack");
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 3) {
				System.out.println("Invalid parameters");
				return;
			} else if (!requests.contains(params[1])) {
				System.out.println("Invalid request type");
				return;
			}
			int requestType = requests.indexOf(params[1]) + 1;
			ErrorThread error = new FirstThread(requestType, Integer.valueOf(params[2]));
			System.out.println("Added FirstThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleLast(String s) {
		try {
			ErrorThread error = new LastThread();
			System.out.println("Added LastThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleSep(String s) {
		try {
			ErrorThread error = new SeparatorThread();
			System.out.println("Added SeparatorThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleTID(String s) {
		List<String> requests = Arrays.asList("data", "ack");
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 3) {
				System.out.println("Invalid parameters");
				return;
			} else if (!requests.contains(params[1])) {
				System.out.println("Invalid request type (Can only be data or ack)");
				return;
			}
			int requestType = requests.indexOf(params[1]) + 3;
			ErrorThread error = new TIDThread(requestType, Integer.valueOf(params[2]));
			System.out.println("Added TIDThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleOpCode(String s) {
		List<String> requests = Arrays.asList("rrq", "wrq", "data", "ack", "rand");
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 4) {
				System.out.println("Invalid parameters");
				return;
			} else if (!requests.contains(params[1])) {
				System.out.println("Invalid request type");
				return;
			}
			int requestType = requests.indexOf(params[1]) + 1;
			int newOpCode = requests.indexOf(params[3]) + 1;
			ErrorThread error = new OpCodeThread(requestType, Integer.valueOf(params[2]), newOpCode);
			System.out.println("Added OpCodeThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	public void handleBlock(String s) {
		List<String> requests = Arrays.asList("rrq", "wrq", "data", "ack");
		try {
			String params[] = s.toLowerCase().trim().split(" ");
			if (params.length < 4) {
				System.out.println("Invalid parameters");
				return;
			} else if (!requests.contains(params[1])) {
				System.out.println("Invalid request type");
				return;

				// Confirm request is data/ack
			} else if (params[1] == requests.get(1) || params[1] == requests.get(2)) {
				System.out.println("Invalid request type");
				return;
			}
			int requestType = requests.indexOf(params[1]) + 1;
			ErrorThread error = new BlockThread(requestType, Integer.valueOf(params[2]), Integer.valueOf(params[3]));
			System.out.println("Added BlockThread to queue.");
			nextThread.add(error);
		} catch (Exception e) {
			System.out.println("Invalid command");
			return;
		}
	}

	/**
	 * Handles commands with parameters
	 * 
	 * @param s,
	 *            user command
	 */
	public void handle(String s) {
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
					if (delay <= Variables.packetTimeout) {
						System.out.println("Delay must be greater than timeout");
						return;
					} else if (requestType == 1 || requestType == 2) {
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
			return;
		}
	}

	/**
	 * Adds a error simulating thread to the queue
	 * 
	 * @param operation,
	 *            lose(1), delay(2) or duplicate(3)
	 * @param packet,
	 *            rrw, wrq, ack or data
	 * @param position,
	 *            position of packet
	 * @param delay,
	 *            amount to delay in ms
	 */
	public void addToQueue(int operation, int packet, int position, int delay) {
		ErrorThread error = null;
		switch (operation) {
		case 1: // lost packet
			error = new LostThread(packet, position);
			System.out.println("Added LostThread to queue.");
			break;
		case 2: // delay packet
			error = new DelayThread(packet, position, delay);
			System.out.println("Added DelayThread to queue.");
			break;
		case 3: // duplication packet
			error = new DuplicatedThread(packet, position, delay);
			System.out.println("Added DuplicatedThread to queue.");
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
		System.out.println("<r> must be 'ack','data', 'wrq', or 'rrq'");
		System.out.println("<r2> must be 'ack','data', 'wrq', 'rrq', or 'rand'");
		System.out.println("<p> position of packet");
		System.out.println("<d> delay in milliseconds");
		System.out.println("<m> mode to change to");
		System.out.println("<f> file to change to");
		System.out.println("The server and client timeout is " + Variables.packetTimeout + "ms");
		System.out.println("\tCommands:");
		System.out.println("\thelp						Prints this message");
		System.out.println("\tverbose 	<on/off>			Turns verbose mode on or off");
		System.out.println("\tquit						Exits the simulator");
		System.out.println("\tmode		<m>				Change mode ");
		System.out.println("\tfile		<f>				Change file ");
		System.out.println("\tfirst		<r> <p>				Remove first byte of specified packet");
		System.out.println("\tlast						Remove last byte of request packet");
		System.out.println("\tsep						Remove byte between file and mode");
		System.out.println("\topcode		<r> <p>	<r2>			Change opcode of specified packet");
		System.out.println("\tblock		<r> <p>	<p>			Change block# of specified packet");
		System.out.println("\ttid		<r> <p>				Change the TID of specified packet");
		System.out.println("\tdelay		<r> <p> <d>			Delays the specified packet by a number of ms");
		System.out.println("\tdup		<r> <p> <d>			Sends a duplicate of the specified packet");
		System.out.println("\tlose		<r> <p>				Loses the specified packet");
	}

}
