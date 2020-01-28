/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on 11/07/2005 00:32:01
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;





/**
 * Normalizes an URL. 
 * Normalization means replacing blank spaces by underlines, 
 * changing special chars by its regular form and etc.
 * @author Rafael Steil
 * @version $Id: URLNormalizer.java,v 1.4 2006/08/20 22:47:42 rafaelsteil Exp $
 */
public class URLNormalizer
{
	public static final int LIMIT = 30;
	
	/**
	 * Normalizes an URL.
	 * The url will be truncated at {@link #LIMIT} chars
	 * @param url the url to normalize
	 * @return the normalized url
	 * @see #normalize(String, int, boolean)
	 */
	public static String normalize(String url)
	{
		return normalize(url, LIMIT, true);
	}
	
	/**
	 * 
	 * @param url the url to normalize
	 * @param limit do not process more than <code>limit + 1</code> chars
	 * @param friendlyTruncate If <code>true</code>, will try to not cut a word if
	 * more than <code>limit</code> chars were processed. It will stop in the next
	 * special char
	 * @return the normalized url
	 */
	public static String normalize(String url, int limit, boolean friendlyTruncate)
	{
		char[] chars = url.toCharArray();
		
		StringBuffer sb = new StringBuffer(url.length());
		
		for (int i = 0; i < chars.length; i++) {
			if (i <= limit || (friendlyTruncate && i > limit && sb.charAt(sb.length() - 1) != '_')) {
				
				if (Character.isSpaceChar(chars[i]) || chars[i] == '-') {
					if (friendlyTruncate && i > limit) {
						break;
					}
					
					if (i > 0 && sb.charAt(sb.length() - 1) != '_') {
						sb.append('_');
					}
				}
				
				if (Character.isLetterOrDigit(chars[i])) {
					sb.append(chars[i]);
				}
				else if (friendlyTruncate && i > limit) {
					break;
				}
			}
		}
		
		return sb.toString().toLowerCase();
	}
	
	private static String USER_AGENT = "Mozilla/5.0";
	// HTTP GET request
	public static String sendGet(String url) throws Exception {
		
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		
		return response.toString();

	}

	// HTTP POST request
	public static String sendPost(String url, String param) throws Exception {

		
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = param;
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		
		return response.toString();


	}

}
