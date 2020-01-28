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
 * Created on 09/08/2007 09:31:17
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.forum;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.jforum.Command;
import net.jforum.ControllerUtils;
import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.PostDAO;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.entities.ModerationLog;
import net.jforum.entities.Post;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.APIException;
import net.jforum.repository.PostRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.search.LuceneManager;
import net.jforum.search.SearchFacade;
import net.jforum.security.SecurityConstants;
import net.jforum.util.AESencrp;
import net.jforum.util.I18n;
import net.jforum.util.SafeHtml;
import net.jforum.util.mail.Spammer;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import net.jforum.view.forum.common.PostCommon;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import freemarker.template.SimpleHash;

/**
 * @author Rafael Steil
 * @version $Id: AjaxAction.java,v 1.7 2007/09/21 15:54:31 rafaelsteil Exp $
 */
public class AjaxAction extends Command
{
	private static Logger logger = Logger.getLogger(AjaxAction.class);
	
	/**
	 * Sends a test message
	 * @param sender The sender's email address
	 * @param host the smtp host
	 * @param auth if need authorization or not
	 * @param username the smtp server username, if auth is needed
	 * @param password the smtp server password, if auth is needed
	 * @param to the recipient
	 * @return The status message
	 */
	public void sendTestMail()
	{
		String sender = this.request.getParameter("sender");
		String host = this.request.getParameter("host");
		String port = this.request.getParameter("port");
		String auth = this.request.getParameter("auth");
		String ssl = this.request.getParameter("ssl");
		String username = this.request.getParameter("username");
		String password = this.request.getParameter("password");
		String to = this.request.getParameter("to");
		
		// Save the current values
		String originalHost = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_HOST);
		String originalAuth = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_AUTH);
		String originalUsername = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_USERNAME);
		String originalPassword = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PASSWORD);
		String originalSender = SystemGlobals.getValue(ConfigKeys.MAIL_SENDER);
		String originalSSL = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_SSL);
		String originalPort = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PORT);
		
		// Now put the new ones
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, host);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, auth);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, username);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, password);
		SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, sender);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, ssl);
		SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, port);
		
		String status = "OK";
		
		// Send the test mail
		class TestSpammer extends Spammer {
			public TestSpammer(String to) {
				List l = new ArrayList();
				
				User user = new User();
				user.setEmail(to);
				
				l.add(user);
				
				this.setUsers(l);
				
				this.setTemplateParams(new SimpleHash());
				this.prepareMessage("JForum Test Mail", null);
			}
			
			protected String processTemplate() throws Exception {
				return ("Test mail from JForum Admin Panel. Sent at " + new Date());
			}
			
			protected void createTemplate(String messageFile) throws Exception {}
		}
		
		Spammer s = new TestSpammer(to);
		
		try {
			s.dispatchMessages();
		}
		catch (Exception e) {
			status = StringEscapeUtils.escapeJavaScript(e.toString());
			logger.error(e.toString(), e);
		}
		finally {
			// Restore the original values
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_HOST, originalHost);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_AUTH, originalAuth);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_USERNAME, originalUsername);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PASSWORD, originalPassword);
			SystemGlobals.setValue(ConfigKeys.MAIL_SENDER, originalSender);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_SSL, originalSSL);
			SystemGlobals.setValue(ConfigKeys.MAIL_SMTP_PORT, originalPort);
		}
		
		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", status);
	}
	
	public void isPostIndexed()
	{
		int postId = this.request.getIntParameter("post_id");

		this.setTemplateName(TemplateKeys.AJAX_IS_POST_INDEXED);
		
		LuceneManager manager = (LuceneManager)SearchFacade.manager();
		Document doc = manager.luceneSearch().findDocumentByPostId(postId);
		
		this.context.put("doc", doc);
	}
	
	public void loadPostContents()
	{
		int postId = this.request.getIntParameter("id");
		PostDAO dao = DataAccessDriver.getInstance().newPostDAO();
		Post post = dao.selectById(postId);
		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}
	
	public void savePost()
	{
		PostDAO postDao = DataAccessDriver.getInstance().newPostDAO();
		Post post = postDao.selectById(this.request.getIntParameter("id"));
		
		String originalMessage = post.getText();
		
		if (!PostCommon.canEditPost(post)) {
			post = PostCommon.preparePostForDisplay(post);
		}
		else {
			post.setText(this.request.getParameter("value"));
			postDao.update(post);
			SearchFacade.update(post);
			post = PostCommon.preparePostForDisplay(post);
		}
		
		boolean isModerator = SecurityRepository.canAccess(SecurityConstants.PERM_MODERATION_POST_EDIT);
		
		if (SystemGlobals.getBoolValue(ConfigKeys.MODERATION_LOGGING_ENABLED)
				&& isModerator && post.getUserId() != SessionFacade.getUserSession().getUserId()) {
			ModerationHelper helper = new ModerationHelper();
			
			this.request.addParameter("log_original_message", originalMessage);
			this.request.addParameter("post_id", String.valueOf(post.getId()));
			this.request.addParameter("topic_id", String.valueOf(post.getTopicId()));
			
			ModerationLog log = helper.buildModerationLogFromRequest();
			log.getPosterUser().setId(post.getUserId());
			
			helper.saveModerationLog(log);
		}
		
		if (SystemGlobals.getBoolValue(ConfigKeys.POSTS_CACHE_ENABLED)) {
			PostRepository.update(post.getTopicId(), PostCommon.preparePostForDisplay(post));
		}
		
		this.setTemplateName(TemplateKeys.AJAX_LOAD_POST);
		this.context.put("post", post);
	}
	
	public void previewPost()
	{
		Post post = new Post();
		
		post.setText(this.request.getParameter("text"));
		post.setSubject(this.request.getParameter("subject"));
		post.setHtmlEnabled("true".equals(this.request.getParameter("html")));
		post.setBbCodeEnabled("true".equals(this.request.getParameter("bbcode")));
		post.setSmiliesEnabled("true".equals(this.request.getParameter("smilies")));
		
		if (post.isHtmlEnabled()) {
			post.setText(new SafeHtml().makeSafe(post.getText()));
		}
		
		post = PostCommon.preparePostForDisplay(post);
		post.setSubject(StringEscapeUtils.escapeJavaScript(post.getSubject()));
		post.setText(StringEscapeUtils.escapeJavaScript(post.getText()));

		this.setTemplateName(TemplateKeys.AJAX_PREVIEW_POST);
		this.context.put("post", post);
	}
	
	/**
	 * @see Command#list()
	 */
	public void list()
	{
		this.ignoreAction();
	}

	public void ssoactivate()
	{
		
	}
	
	public void ssoinsert()
	{
		try {
			
			String enc_data = this.request.getParameter("enc_data");
			String key_data = this.request.getParameter("key_data");
			
			String enc = AESencrp.pwdDigest(key_data);
			if(enc.equals(enc_data)){
				String username = this.request.getParameter("username");
				String email = this.request.getParameter("email");
				String password = this.request.getParameter("password");
				
				if (username.length() > SystemGlobals.getIntValue(ConfigKeys.USERNAME_MAX_LENGTH)) {
					throw new APIException(I18n.getMessage("User.usernameTooBig"));
				}
				
				if (username.indexOf('<') > -1 || username.indexOf('>') > -1) {
					throw new APIException(I18n.getMessage("User.usernameInvalidChars"));
				}
				
				UserDAO dao = DataAccessDriver.getInstance().newUserDAO();

				if (dao.isUsernameRegistered(username)) {
					throw new APIException(I18n.getMessage("UsernameExists"));
				}
				
				if (dao.findByEmail(email) != null) {
					throw new APIException(I18n.getMessage("User.emailExists", new Object[] { email }));
				}
				
				// Ok, time to insert the user
				User user = new User();
				user.setUsername(username);
				user.setEmail(email);
				user.setPassword(password);
				user.setActive(1);
				
				int userId = dao.addNew(user);
				
				dao.writeUserActive(userId);
				
				this.setTemplateName(TemplateKeys.API_USER_INSERT);
				this.context.put("userId", new Integer(userId));
				
				
	            this.response.setHeader("Access-Control-Allow-Origin", "*");
	            this.response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS");
	           // this.response.setStatus(HttpServletResponse.SC_OK);
			}
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
		}
		
		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", "OK");
	}
	
	
	
	public void ssoinsert_login()
	{
		try {
			String username = this.request.getParameter("username");
			String email = this.request.getParameter("email");
			String apiKey = this.request.getParameter("apiKey");
			String password = this.request.getParameter("password");
			String enc_data = this.request.getParameter("enc_data");
			String key_data = this.request.getParameter("key_data");
			
			String enc = AESencrp.pwdDigest(key_data);
			if(enc.equals(enc_data)){
			
				
				if (username.length() > SystemGlobals.getIntValue(ConfigKeys.USERNAME_MAX_LENGTH)) {
					throw new APIException(I18n.getMessage("User.usernameTooBig"));
				}
				
				if (username.indexOf('<') > -1 || username.indexOf('>') > -1) {
					throw new APIException(I18n.getMessage("User.usernameInvalidChars"));
				}
				
				UserDAO dao = DataAccessDriver.getInstance().newUserDAO();

				if (dao.isUsernameRegistered(username)) {
					throw new APIException(I18n.getMessage("UsernameExists"));
				}
				
				if (dao.findByEmail(email) != null) {
					throw new APIException(I18n.getMessage("User.emailExists", new Object[] { email }));
				}
				
				// Ok, time to insert the user
				User user = new User();
				user.setUsername(username);
				user.setEmail(email);
				user.setPassword(password);
				user.setActive(1);
				user.setApiKey(apiKey);
				
				int userId = dao.addNew(user);
				
				dao.writeUserActive(userId);
				
				this.setTemplateName(TemplateKeys.API_USER_INSERT);
				this.context.put("userId", new Integer(userId));
				
				
	            this.response.setHeader("Access-Control-Allow-Origin", "*");
	            this.response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS");
	           // this.response.setStatus(HttpServletResponse.SC_OK);
	            
	        	validateLoginByName(username);
			}
		}
		catch (Exception e) {
			this.setTemplateName(TemplateKeys.API_ERROR);
			this.context.put("exception", e);
		}
		
	
		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", "OK");
	}
	
	
	
	public void ssologout()
	{
		
		UserSession userSession = SessionFacade.getUserSession();
		SessionFacade.storeSessionData(userSession.getSessionId(), JForumExecutionContext.getConnection());

		SessionFacade.makeUnlogged();
		SessionFacade.remove(userSession.getSessionId());
		
		
		// Disable auto login
		userSession.setAutoLogin(false);
		userSession.makeAnonymous();

		SessionFacade.add(userSession);
		
		this.setTemplateName(TemplateKeys.AJAX_TEST_MAIL);
		this.context.put("status", "OK");
	}
	
	
	public void validateLoginByName(String username)
	{
		
		boolean validInfo = false;
		
		UserDAO um = DataAccessDriver.getInstance().newUserDAO();
        User user =  um.selectByName(username);

		if (user != null) {
			
			SessionFacade.makeLogged();
			
			String sessionId = SessionFacade.isUserInSession(user.getId());
			UserSession userSession = new UserSession(SessionFacade.getUserSession());
			
			// Remove the "guest" session
			SessionFacade.remove(userSession.getSessionId());
			
			userSession.dataToUser(user);
			
			UserSession currentUs = SessionFacade.getUserSession(sessionId);

            UserSession tmpUs;
			if (sessionId != null && currentUs != null) {
				// Write its old session data
				SessionFacade.storeSessionData(sessionId, JForumExecutionContext.getConnection());
				tmpUs = new UserSession(currentUs);
				SessionFacade.remove(sessionId);
			}
			else {
				UserSessionDAO sm = DataAccessDriver.getInstance().newUserSessionDAO();
				tmpUs = sm.selectById(userSession, JForumExecutionContext.getConnection());
			}

			I18n.load(user.getLang());

			// Remove cookies for safety
			ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_USER_HASH), null);
			ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_AUTO_LOGIN), null);
		
			if (tmpUs == null) {
				userSession.setLastVisit(new Date(System.currentTimeMillis()));
			}
			else {
				// Update last visit and session start time
				userSession.setLastVisit(new Date(tmpUs.getStartTime().getTime() + tmpUs.getSessionTime()));
			}
			
			SessionFacade.add(userSession);
			SessionFacade.setAttribute(ConfigKeys.TOPICS_READ_TIME, new HashMap());
			ControllerUtils.addCookie(SystemGlobals.getValue(ConfigKeys.COOKIE_NAME_DATA), 
				Integer.toString(user.getId()));

			SecurityRepository.load(user.getId(), true);
			validInfo = true;
		}

	}
	
	public void getUnRead(){
		int total_cnt = 0;
		String username = this.request.getParameter("username");
		if(username != null){
			User user =  DataAccessDriver.getInstance().newUserDAO().selectByName(username);
			if(user != null){
				total_cnt = DataAccessDriver.getInstance().newPrivateMessageDAO().selectUnReadCnt(user);			
			}	
		}
		this.setTemplateName(TemplateKeys.AJAX_LOAD_UNREAD);
		this.context.put("unread", total_cnt);
	}
	
}
