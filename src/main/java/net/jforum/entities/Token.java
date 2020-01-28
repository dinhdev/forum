package net.jforum.entities;

import java.io.Serializable;

public class Token implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String token;
	private String expires;
	private long id;
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getExpires() {
		return expires;
	}
	public void setExpires(String expires) {
		this.expires = expires;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
