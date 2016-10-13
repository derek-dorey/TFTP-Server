package com.sysc.tftp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import java.io.*;
import com.sysc.tftp.utils.Variables;

public class ClientInterface {

	public static void main(String[] args) {
		if (Arrays.asList(args).contains(Variables.TEST_MODE_FLAG)) {
			Variables.CLIENT_MODE = Variables.Mode.TEST;
		}
		if (Arrays.asList(args).contains(Variables.VERBOSE_FLAG)) {
			Variables.VERBOSE = true;
		}

		Client c = new Client();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println();
			System.out.println("[ SYSC 3303 TFTP Client ]");
			System.out.println();
			System.out.println("(1) Read file");
			System.out.println("(2) Write to file");
			System.out.println("(3) Exit");
			System.out.println();
			System.out.print("Enter selection: ");

			int choice = -1;
			try {
				choice = Integer.parseInt(br.readLine());
			} catch (Exception e) {
				System.out.println("\nInvalid input.\n");
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
			
			String filePath = (Variables.CLIENT_FILES_DIR + file); //combine directory and filename to locate file
			
			File f = new File(filePath); //create file object using the filePath above

			if (choice == 1) {
				
				if(f.exists() && !f.isDirectory()) { 
					System.out.println("File already exists.");  //if the file already exists in the client directory, do not send RRQ and re-prompt user input
					continue;									 //Note: do not send error code 6, rather terminate the request
				}
				else {
					c.receiveFile(file);  //file is a valid input and does not already exist. Proceed with RRQ
				}
				
			} else if (choice == 2) {
				c.sendFile(file);
			}
			System.out.println();
		}
	}

}
