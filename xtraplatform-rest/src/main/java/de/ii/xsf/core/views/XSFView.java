package de.ii.xsf.core.views;

import io.dropwizard.views.View;

/**
 *
 * @author fischer
 */
public class XSFView extends View {

    private final String uri;
    private String token;
    
    public XSFView(String template, String uri, String token) {
        super(template + ".mustache");
        this.uri = uri;
        this.token = "";
        if (token != null && !token.isEmpty()) {
            this.token = "?token=" + token;
        }
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
}
