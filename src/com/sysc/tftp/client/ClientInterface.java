package com.sysc.tftp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sysc.tftp.utils.Variables;

public class ClientInterface {

	public static void main(String[] args) {
		Client c = new Client();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println();
			System.out.println("[ SYSC 3303 TFTP Client ]");
			System.out.println();
			System.out.println("Current mode: [" + Variables.CLIENT_MODE.getType() + "]");
			System.out.println("Verbose: [" + (Variables.VERBOSE ? "ON" : "OFF") + "]");
			System.out.println();
			System.out.println("(1) Read file");
			System.out.println("(2) Write to file");
			System.out.println("(3) Exit");
			System.out.println();
			System.out.print("Enter selection: ");

			int choice = -1;
			String line = null;
			try {
				line = br.readLine();
				choice = Integer.parseInt(line);
			} catch (Exception e) {
				String settings = line;
				switch (settings.toLowerCase().trim()) {
				case Variables.SET_MODE_NORMAL:
					Variables.CLIENT_MODE = Variables.Mode.NORMAL;
					System.out.println("\nChanged mode.\n");
					break;
				case Variables.SET_MODE_TEST:
					Variables.CLIENT_MODE = Variables.Mode.TEST;
					System.out.println("\nChanged mode.\n");
					break;
				case Variables.SET_VERBOSE_ON:
					Variables.VERBOSE = true;
					System.out.println("\nVerbose settings changed.\n");
					break;
				case Variables.SET_VERBOSE_OFF:
					Variables.VERBOSE = false;
					System.out.println("\nVerbose settings changed.\n");
					break;
				default:
					System.out.println("\nInvalid input.\n");
				}
				continue;
			}
			System.out.println();

			if (choice <= 0 || choice >= 4) {
				System.out.println("\nInvalid input.\n");
				continue;
			} else if (choice == 3) {
				System.out.println("Exiting...");
				break;
			}

			String file = null;
			try {
				System.out.println("Enter file: ");
				file = br.readLine();
				System.out.println("File selected: " + file);
				System.out.println();
			} catch (IOException e) {
				System.out.println("\nInvalid input.\n");
				continue;
			}

			if (file == null || "".equals(file)) {
				System.out.println("\nInvalid input.\n");
				continue;
			}

			if (choice == 1) {
				c.receiveFile(file);
			} else if (choice == 2) {
				c.sendFile(file);
			}
			System.out.println();
		}
	}

}
