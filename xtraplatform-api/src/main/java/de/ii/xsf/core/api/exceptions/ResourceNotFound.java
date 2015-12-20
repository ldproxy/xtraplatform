package de.ii.xsf.core.api.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class ResourceNotFound extends XtraserverFrameworkException {

    public ResourceNotFound(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public ResourceNotFound() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = this.code;             
        this.stdmsg = "ResourceNotFound";
    }
    
    public ResourceNotFound(String msg) {
        this();
        this.msg = msg;
    }
    
    public ResourceNotFound(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
}