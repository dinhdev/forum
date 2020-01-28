package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.TokenDAO;
import net.jforum.dao.UserDAO;
import net.jforum.entities.Forum;
import net.jforum.entities.Token;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

public class GenericTokenDAO extends AutoKeys implements TokenDAO {

	@Override
	public Token selectByToken(String token) {

		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			p = JForumExecutionContext.getConnection().prepareStatement("select * from jforum_tokens where token=?");

			p.setString(1, token);
			rs = p.executeQuery();
			Token f = new Token();

			if (rs.next()) {
				f.setToken(rs.getString("token"));
				f.setExpires(rs.getString("expires"));
				
			}
			return f;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, p);
		}
	}

	@Override
	public int insertToken(Token token) {
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			
			String sql = "insert into jforum_tokens (token,expires) values('"+token.getToken()+"','"+token.getExpires()+"')";
			p = JForumExecutionContext.getConnection().prepareStatement(sql);

			p.execute();
			
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, p);
		}
		return 0;
	}

}
