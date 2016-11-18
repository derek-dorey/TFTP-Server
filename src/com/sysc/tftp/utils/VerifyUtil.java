package com.sysc.tftp.utils;

import com.sysc.tftp.utils.Variables.Request;

public class VerifyUtil {

	/**
	 * Verifies that the request is valid
	 * 
	 * @param data
	 * 
	 * @return Enum of type of request
	 */
	public static Request verifyRequest(byte[] data, int len) {
		
		Request req; // Request type from packet
		int j = 0, k = 0;	//Used for positions within the packet 

		//Check first byte, must be 0	
		if (data[0] != 0) {
			
			//Invalid request type
			return null;
		
		//Check second byte, is it read?
		} else if (data[1] == 1) {
			
			//Read request detected
			req = Request.RRQ;
		
		//Check second byte, is it write?
		} else if (data[1] == 2) {
			
			//Write request detected
			req = Request.WRQ;
			
		//Check second byte, is it data?
		} else if (data[1] == 3) {
			
			//Read request detected
			req = Request.DATA;
		
		//Check second byte, is it ACK?
		} else if (data[1] == 4) {
			
			//Write request detected
			req = Request.ACK;
	
		//Check second byte, is it ERROR?
		} else if (data[1] == 5) {
			
			//Write request detected
			req = Request.ERROR;
				
		//Unknown request type
		} else {
			
			//Return unknown request
			return null;
		}

		//If read or write request, verify filename and mode
		if (req == Request.RRQ || req == Request.WRQ) {
			
			// search for next all 0 byte
			for (j = 2; j < len; j++) {
				if (data[j] == 0) {
					break;
				}
			}
			
			//Check if we went to end of string without 0 byte
			if (j == len) {
				
				//No zero byte found, not right format
				return null;
			}
			
			//If we didn't loop at all
			if (j == 2) {
				
				//No filename specified
				return null;
			}
			
			
			//Loop until next zero byte to get the mode
			for (k = j + 1; k < len; k++) {
				if (data[k] == 0) {
					break;
				}
			}
			
			//If we didn't end on a zero byte not formatted right
			if (k == len) {
				
				//Not valid request
				return null;
			}
			
			//If we didn't get anywhere looping for mode, its not specified
			if (k == j + 1) {
				
				//Not a valid request
				return null;
				
			}
									
		}

		//Return the request type we got from the packet
		return req;
		
	}
	
}
