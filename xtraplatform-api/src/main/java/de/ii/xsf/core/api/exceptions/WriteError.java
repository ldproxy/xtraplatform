/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.ii.xsf.core.api.exceptions;

import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class WriteError extends XtraserverFrameworkException {

    public WriteError(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public WriteError() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.NOT_FOUND;
        this.htmlCode = this.code; 
        this.stdmsg = "WriteError";
    }

    public WriteError(String msg) {
        this();
        this.msg = msg;
    }
}
