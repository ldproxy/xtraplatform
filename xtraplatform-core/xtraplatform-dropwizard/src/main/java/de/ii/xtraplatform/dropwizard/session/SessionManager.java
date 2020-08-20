/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.api.session;

import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.SessionIdManager;

/**
 *
 * @author zahnen
 */

// TODO: jetty implementation was simplififed in 9.4: http://www.eclipse.org/jetty/documentation/9.4.x/session-management.html

// TODO: only used for remember me, replace with client-side sessions, see https://index.scala-lang.org/softwaremill/akka-http-session/core/0.5.4?target=_2.12
public interface SessionManager {

    /*public org.eclipse.jetty.server.SessionManager getSessionManager();

    public SessionIdManager getSessionIdManager();

    public void saveSession(HttpSession session);*/
}
