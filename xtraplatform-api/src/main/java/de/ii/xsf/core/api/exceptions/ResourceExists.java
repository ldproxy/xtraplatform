package de.ii.xsf.core.api.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class ResourceExists extends XtraserverFrameworkException {

    public ResourceExists(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public ResourceExists() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.BAD_REQUEST;
        this.htmlCode = this.code;             
        this.stdmsg = "ResourceExists";
    }
    
    public ResourceExists(String msg) {
        this();
        this.msg = msg;
    }
    
    public ResourceExists(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
}