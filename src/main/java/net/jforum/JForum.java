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
 * This file creation date: Mar 3, 2003 / 11:43:35 AM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jforum.context.JForumContext;
import net.jforum.context.RequestContext;
import net.jforum.context.ResponseContext;
import net.jforum.context.web.WebRequestContext;
import net.jforum.context.web.WebResponseContext;
import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.MySQLVersionWorkarounder;
import net.jforum.dao.TokenDAO;
import net.jforum.dao.UserDAO;
import net.jforum.dao.UserSessionDAO;
import net.jforum.entities.Banlist;
import net.jforum.entities.Token;
import net.jforum.entities.User;
import net.jforum.entities.UserSession;
import net.jforum.exceptions.ExceptionWriter;
import net.jforum.exceptions.ForumStartupException;
import net.jforum.repository.BanlistRepository;
import net.jforum.repository.ModulesRepository;
import net.jforum.repository.RankingRepository;
import net.jforum.repository.SecurityRepository;
import net.jforum.repository.SmiliesRepository;
import net.jforum.util.I18n;
import net.jforum.util.MD5;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.preferences.TemplateKeys;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * Front Controller.
 * 
 * @author Rafael Steil
 * @version $Id: JForum.java,v 1.116 2007/10/10 04:54:20 rafaelsteil Exp $
 */
public class JForum extends JForumBaseServlet 
{
	private static boolean isDatabaseUp;
	
	/**
	 * @see javax.servlet.Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		super.startApplication();
		
		// Start database
		isDatabaseUp = ForumStartup.startDatabase();
		
		try {
			Connection conn = DBConnection.getImplementation().getConnection();
			conn.setAutoCommit(!SystemGlobals.getBoolValue(ConfigKeys.DATABASE_USE_TRANSACTIONS));
			
			// Try to fix some MySQL problems
			MySQLVersionWorkarounder dw = new MySQLVersionWorkarounder();
			dw.handleWorkarounds(conn);
			
			// Continues loading the forum
			JForumExecutionContext ex = JForumExecutionContext.get();
			ex.setConnection(conn);
			JForumExecutionContext.set(ex);
			
			// Init general forum stuff
			ForumStartup.startForumRepository();
			RankingRepository.loadRanks();
			SmiliesRepository.loadSmilies();
			BanlistRepository.loadBanlist();
		}
		catch (Throwable e) {
            e.printStackTrace();
            throw new ForumStartupException("Error while starting jforum", e);
		}
		finally {
			JForumExecutionContext.finish();
		}
	}
	
	/**
	 * @see javax.servlet.http.HttpServlet#service(HttpServletRequest, HttpServletResponse)
	 */
	public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
	{
		/**
		 * check sso authentication
		 */
		
		Writer out = null;
		JForumContext forumContext = null;
		RequestContext request = null;
		ResponseContext response = null;
		String encoding = SystemGlobals.getValue(ConfigKeys.ENCODING);

		try {
			// Initializes the execution context
			JForumExecutionContext ex = JForumExecutionContext.get();
			
			
			request = new WebRequestContext(req);
            response = new WebResponseContext(res);

			this.checkDatabaseStatus();

            forumContext = new JForumContext(request.getContextPath(),
                SystemGlobals.getValue(ConfigKeys.SERVLET_EXTENSION),
                request,
                response
            );
            ex.setForumContext(forumContext);

            JForumExecutionContext.set(ex);

            String token = req.getParameter("token");
            String username = req.getParameter("username");
            boolean logged = SessionFacade.isLogged();
            
        	TokenDAO dao = DataAccessDriver.getInstance().newTokenDAO();
        	
        	String contextPath = request.getRequestURI();
        	  
            if(!logged) {
                if (token == null) {
                    try {
                        String new_token = UUID.randomUUID().toString().toLowerCase();
                        
                        Token token_model = new Token();
                        token_model.setToken(new_token);
                        token_model.setExpires((System.currentTimeMillis() + 1000 * 60 * 10) + "");
                      	
                    	dao.insertToken(token_model);

                    	String domain = SystemGlobals.getValue(ConfigKeys.SSOAUTH_DOMAIN);
                    	String auth_url = SystemGlobals.getValue(ConfigKeys.SSOAUTH_AUTHURL);
                		
                        String redirect = URLEncoder.encode(domain + req.getServletPath(), "UTF-8");

                        String url = auth_url + "?redirect=" + redirect + "&token=" + new_token;
                        
                        
                        if(contextPath.indexOf("user/login.page") >= 0 
                        		|| contextPath.indexOf("user/ssologin.page") >= 0
                        		|| contextPath.indexOf("user/logout.page") >= 0
                        		|| contextPath.indexOf("jforum.page") >= 0
                        		){
                        	
                        }
                        else{
                        	res.sendRedirect(url);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();

                    }
                }

                Token current_token = dao.selectByToken(token);

                if (username != null) {

                    if (current_token == null) {
                        
                    }
                    else{
    	                long current_time = System.currentTimeMillis();
    	                if(current_token.getExpires() == null)
    	                	current_token.setExpires("0");
    	                
    	                long diff = current_time - Long.parseLong(current_token.getExpires());
    	
    	                if (diff > 0) {
    	                   
    	                }
    	                else{
    	                	this.validateLogin(username);
    	                }
                    }
                }
            }
            
          
            
			// Setup stuff
			SimpleHash context = JForumExecutionContext.getTemplateContext();
			
			  // set integration system url
        	String dataexo_url = SystemGlobals.getValue(ConfigKeys.SSOAUTH_DATAEXO);
        	context.put("dataexo_url", dataexo_url);
            //
        	
			ControllerUtils utils = new ControllerUtils();
			utils.refreshSession();
			
			context.put("logged", SessionFacade.isLogged());
		  	
			// Process security data
			SecurityRepository.load(SessionFacade.getUserSession().getUserId());

			utils.prepareTemplateContext(context, forumContext);

			String module = request.getModule();

			/*if(contextPath.equals("/jforum.page")){
				if(module == null){
					module = "user";
					Map query = new HashMap();
					query.put("redirect" , "");
					query.put("username" , "");
					query.put("password" , "");
					query.put("module" , "user");
					query.put("action" , "validateLogin");
					query.put("login" , "");

					WebRequestContext reqTmp = (WebRequestContext) request;

					reqTmp.setQuery(query);

					//request = reqTmp;
				}
			}
			*/
			// Gets the module class name
			String moduleClass = module != null 
				? ModulesRepository.getModuleClass(module) 
				: null;

			if (moduleClass == null) {
				// Module not found, send 404 not found response
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			else {
				boolean shouldBan = this.shouldBan(request.getRemoteAddr());
				
				if (!shouldBan) {
					context.put("moduleName", module);


						context.put("action", request.getAction());

				}
				else {
					moduleClass = ModulesRepository.getModuleClass("forums");
					context.put("moduleName", "forums");
					((WebRequestContext)request).changeAction("banned");
				}
				
				if (shouldBan && SystemGlobals.getBoolValue(ConfigKeys.BANLIST_SEND_403FORBIDDEN)) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
				else {
					context.put("language", I18n.getUserLanguage());
					context.put("session", SessionFacade.getUserSession());
					context.put("request", req);
					context.put("response", response);
					
					out = this.processCommand(out, request, response, encoding, context, moduleClass);
				}
			}
		}
		catch (Exception e) {
			this.handleException(out, response, encoding, e, request);
		}
		finally {
			this.handleFinally(out, forumContext, response);
		}		
	}

	private Writer processCommand(Writer out, RequestContext request, ResponseContext response, 
			String encoding, SimpleHash context, String moduleClass) throws Exception
	{
		// Here we go, baby
		Command c = this.retrieveCommand(moduleClass);
		Template template = c.process(request, response, context);

		if (JForumExecutionContext.getRedirectTo() == null) {
			String contentType = JForumExecutionContext.getContentType();
			
			if (contentType == null) {
				contentType = "text/html; charset=" + encoding;
			}
			
			response.setContentType(contentType);
			
			// Binary content are expected to be fully 
			// handled in the action, including outputstream
			// manipulation
			if (!JForumExecutionContext.isCustomContent()) {
				out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), encoding));
				template.process(JForumExecutionContext.getTemplateContext(), out);
				out.flush();
			}
		}
		
		return out;
	}

	private void checkDatabaseStatus()
	{
		if (!isDatabaseUp) {
			synchronized (this) {
				if (!isDatabaseUp) {
					isDatabaseUp = ForumStartup.startDatabase();
				}
			}
		}
	}

	private void handleFinally(Writer out, JForumContext forumContext, ResponseContext response) throws IOException
	{
		try {
			if (out != null) { out.close(); }
		}
		catch (Exception e) {
		    // catch close error 
		}
		
		String redirectTo = JForumExecutionContext.getRedirectTo();
		JForumExecutionContext.finish();
		
		if (redirectTo != null) {
			if (forumContext != null && forumContext.isEncodingDisabled()) {
				response.sendRedirect(redirectTo);
			} 
			else {
				response.sendRedirect(response.encodeRedirectURL(redirectTo));
			}
		}
	}

	private void handleException(Writer out, ResponseContext response, String encoding, 
		Exception e, RequestContext request) throws IOException
	{
		JForumExecutionContext.enableRollback();
		
		if (e.toString().indexOf("ClientAbortException") == -1) {
			response.setContentType("text/html; charset=" + encoding);
			if (out != null) {
				new ExceptionWriter().handleExceptionData(e, out, request);
			}
			else {
				new ExceptionWriter().handleExceptionData(e, new BufferedWriter(new OutputStreamWriter(response.getOutputStream())), request);
			}
		}
	}
	
	private boolean shouldBan(String ip)
	{
		Banlist b = new Banlist();
		
		b.setUserId(SessionFacade.getUserSession().getUserId());
		b.setIp(ip);
		
		return BanlistRepository.shouldBan(b);
	}

	private Command retrieveCommand(String moduleClass) throws Exception
	{
		return (Command)Class.forName(moduleClass).newInstance();
	}
	
	/** 
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	public void destroy() {
		super.destroy();
		System.out.println("Destroying JForum...");
		
		try {
			DBConnection.getImplementation().realReleaseAllConnections();
			ConfigLoader.stopCacheEngine();
		}
		catch (Exception e) { }
	}
	
	
	public void validateLogin(String username)
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
}
