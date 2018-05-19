/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.exceptions;

import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class BadRequest extends XtraserverFrameworkException {

    public BadRequest(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public BadRequest() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.BAD_REQUEST;
        this.htmlCode = this.code;             
        this.stdmsg = "BadRequest";
    }
    
    public BadRequest(String msg) {
        this();
        this.msg = msg;
    }
    
    public BadRequest(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
}