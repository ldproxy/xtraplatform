/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.views;

import de.ii.xsf.logging.XSFLogger;
import io.dropwizard.views.View;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.net.URI;

/**
 *
 * @author fischer
 */
public class GenericView extends View {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GenericView.class);

    private final URI uri;
    private Object data;
    
    public GenericView(String template, URI uri) {
        super(template + ".mustache");
        this.uri = uri;
        this.data = null;
    }

    public GenericView(String template, URI uri, Object data) {
        this(template, uri);
        this.data = data;
    }

    public String getPath() {
        if (uri.getPath().endsWith("/")) {
            return "";
        } else {           
            return uri.getPath().substring( uri.getPath().lastIndexOf("/")+1)+"/";
        }
    }

    public String getQuery() {
        return "?" + (uri.getQuery() != null ? uri.getQuery() + "&" : "");
    }

    public Object getData() {
        return data;
    }
}
