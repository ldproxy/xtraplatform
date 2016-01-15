/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
