package de.ii.xsf.core.api.session;

import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.SessionIdManager;

/**
 *
 * @author zahnen
 */
public interface SessionManager {

    public org.eclipse.jetty.server.SessionManager getSessionManager();

    public SessionIdManager getSessionIdManager();

    public void saveSession(HttpSession session);
}
