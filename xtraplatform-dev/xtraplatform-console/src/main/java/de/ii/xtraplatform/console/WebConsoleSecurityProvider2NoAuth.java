/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.console;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.webconsole.WebConsoleSecurityProvider2;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class WebConsoleSecurityProvider2NoAuth implements WebConsoleSecurityProvider2 {

    @Override
    public boolean authenticate(HttpServletRequest hsr, HttpServletResponse hsr1) {
        return true;
    }

    @Override
    public Object authenticate(String string, String string1) {
        return "";
    }

    @Override
    public boolean authorize(Object o, String string) {
        return true;
    }  
}
