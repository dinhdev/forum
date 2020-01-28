/*
 * Created on Mar 15, 2005 1:22:52 PM
 */
package net.jforum.cache;

import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.TreeCacheListener;
import org.jgroups.View;

/**
 * @author Rafael Steil
 * @version $Id: JBossCacheListener.java,v 1.4 2007/08/01 22:30:06 rafaelsteil Exp $
 */
public class JBossCacheListener implements TreeCacheListener
{
	/**
	 * @see TreeCacheListener#nodeCreated(Fqn)
	 */
	public void nodeCreated(Fqn fqn) {}
	
	/**
	 * @see TreeCacheListener#nodeRemoved(Fqn)
	 */
	public void nodeRemoved(Fqn fqn) {}

	/**
	 * @see TreeCacheListener#nodeLoaded(Fqn)
	 */
	public void nodeLoaded(Fqn fqn) {}

	/**
	 * @see TreeCacheListener#nodeEvicted(Fqn)
	 */
	public void nodeEvicted(Fqn fqn) {}

	/**
	 * @see TreeCacheListener#nodeModified(Fqn)
	 */
	public void nodeModified(Fqn fqn)
	{
		if (CacheEngine.NOTIFICATION.startsWith((String)fqn.get(0))) {
		}
	}

	/**
	 * @see TreeCacheListener#nodeVisited(Fqn)
	 */
	public void nodeVisited(Fqn fqn) {}

	/**
	 * @see TreeCacheListener#cacheStarted(TreeCache)
	 */
	public void cacheStarted(TreeCache cache) {}

	/**
	 * @see TreeCacheListener#cacheStopped(TreeCache)
	 */
	public void cacheStopped(TreeCache cache) {}

	/**
	 * @see TreeCacheListener#viewChange(View)
	 */
	public void viewChange(View view) {}
}
