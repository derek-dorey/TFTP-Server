package com.sysc.tftp.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.sysc.tftp.utils.Variables;

public class ClientInterface {

	public static void main(String[] args) {
		try {
			Variables.serverIP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			return;
		}
		Client c = new Client();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		printHelp();
		while (true) {
			String line = null;
			try {
				line = br.readLine().toLowerCase().trim();
				
				if ("quit".equals(line)) {
					System.out.println("Exiting...");
					break;
				} else if ("help".equals(line)) {
					printHelp();
				} else if (line.contains("verbose")) {
					try {
						String setting = line.split(" ")[1].toLowerCase().trim();
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
				} else if (line.contains("mode")) {
					try {
						String setting = line.split(" ")[1].toLowerCase().trim();
						if ("normal".equals(setting)) {
							Variables.CLIENT_MODE = Variables.Mode.NORMAL;
							System.out.println("Current mode: [" + Variables.CLIENT_MODE.getType() + "]");
						} else if ("test".equals(setting)) {
							Variables.CLIENT_MODE = Variables.Mode.TEST;
							System.out.println("Current mode: [" + Variables.CLIENT_MODE.getType() + "]");
						}
					} catch (Exception e) {
						continue;
					}
				} else if (line.toLowerCase().contains("server")) {
					String[] params = line.split(" ");
					String ip = params[1];
					if ("localhost".equals(ip)) {
						Variables.serverIP = InetAddress.getLocalHost();
					} else {
						Variables.serverIP = InetAddress.getByName(ip);
					}
					System.out.println("Changed server ip to: " + ip);
				} else {
					String[] params = line.split(" ");
					if (params[0].equals("read")) {
						c.receiveFile(params[1]);
					} else if (params[0].equals("write")) {
						c.sendFile(params[1]);
					}
					System.out.println("Done.\n");
				}
			} catch (Exception e) {
				System.out.println("Invalid command");
				continue;
			}
		}
	}
	
	/**
	 * Prints help for console commands
	 */
	public static void printHelp() {
		System.out.println("[ SYSC 3303 TFTP Client ]");
		System.out.println("<f> filename");
		System.out.println("<i> ip address");
		System.out.println("\tCommands:");
		System.out.println("\thelp					Prints this message");
		System.out.println("\tverbose		<on/off>		Turns verbose mode on or off");
		System.out.println("\tmode		<normal/test>		Turns mode to test or normal");
		System.out.println("\tquit					Exits the client");
		System.out.println("\tread		<f>			Read file");
		System.out.println("\twrite		<f>			Write file");
		System.out.println("\tserver		<i>			Change server ip address to hit");
		System.out.println("Current mode: [" + Variables.CLIENT_MODE.getType() + "]");
		System.out.println("Verbose mode: [" + (Variables.VERBOSE ? "ON" : "OFF") + "]");
		System.out.println();
	}

}
