package com.sysc.tftp.utils;

import com.sysc.tftp.utils.Variables.Request;

public class VerifyUtil {

	/**
	 * Verifies that the request is valid
	 * 
	 * @param data
	 *            Request sent
	 * @return Enum of type of request
	 */
	public static Request verifyRequest(byte[] data, int len) {
		Request req; // READ, WRITE or ERROR
		int j = 0, k = 0;

		if (data[0] != 0) {
			return Request.ERROR; // bad
		} else if (data[1] == 1) {
			req = Request.RRQ; // could be read
		} else if (data[1] == 2) {
			req = Request.WRQ; // could be write
		} else {
			return Request.ERROR; // bad
		}

		if (req != Request.ERROR) { // check for filename
			// search for next all 0 byte
			for (j = 2; j < len; j++) {
				if (data[j] == 0) {
					break;
				}
			}
			if (j == len) {
				return Request.ERROR; // didn't find a 0 byte
			}
			if (j == 2) {
				return Request.ERROR; // filename is 0 bytes long
			}
		}

		if (req != Request.ERROR) { // check for mode
			// search for next all 0 byte
			for (k = j + 1; k < len; k++) {
				if (data[k] == 0) {
					break;
				}
			}
			// TODO verify mode is default mode: octet (currently)
			
			if (k == len) {
				return Request.ERROR; // didn't find a 0 byte
			}
			if (k == j + 1) {
				return Request.ERROR; // mode is 0 bytes long
			}
		}

		if (k != len - 1) {
			return Request.ERROR; // other stuff at end of packet
		}

		return req;
	}
	
	public static Request verifyPacket(byte[] data) {
		Request req; // ACK, DATA or ERROR
		if (data[0] != 0) {
			return Request.ERROR; // bad
		} else if (data[1] == 3) {
			req = Request.DATA; // could be data
		} else if (data[1] == 4) {
			req = Request.ACK; // could be ack
		} else {
			return Request.ERROR; // bad
		}
		
		// TODO
		// other checks
		// should we check block number here aswell?
		
		return req;
	}
	
}
