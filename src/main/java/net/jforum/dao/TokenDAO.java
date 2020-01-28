package net.jforum.dao;

import java.util.List;

import net.jforum.entities.Forum;
import net.jforum.entities.Token;

public interface TokenDAO 
{
	
	public Token selectByToken(String token) ;
	
	public int insertToken(Token token) ;
	
}
