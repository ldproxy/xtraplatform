package de.ii.xsf.core.api.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class PasswordNotValid extends XtraserverFrameworkException {

    public PasswordNotValid(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public PasswordNotValid() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.INTERNAL_SERVER_ERROR;
        this.htmlCode = this.code;             
        this.stdmsg = "ResourceNotFound";
    }
    
    public PasswordNotValid(String msg) {
        this();
        this.msg = msg;
    }
    
    public PasswordNotValid(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
}