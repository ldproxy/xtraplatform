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

import io.dropwizard.views.View;

/**
 *
 * @author fischer
 */
public class GenericView extends View {

    private final String uri;
    private String token;
    private Object directory;
    
    public GenericView(String template, String uri, String token) {
        super(template + ".mustache");
        this.uri = uri;
        this.token = "";
        if (token != null && !token.isEmpty()) {
            this.token = "?token=" + token;
        }
        this.directory = null;
    }

    public GenericView(Object directory, String template, String uri, String token) {
        this(template, uri, token);
        this.directory = directory;
    }

    public String getPrefix() {
        if (uri.endsWith("/")) {
            return "";
        } else {           
            return uri.substring( uri.lastIndexOf("/")+1)+"/";
        }
    }
    
    public String getToken() {
        return token;
    }

    public String getJsonQuery() {
        return token.isEmpty() ? "?f=json" : token + "&f=json";
    }

    public Object getDirectory() {
        return directory;
    }
}
