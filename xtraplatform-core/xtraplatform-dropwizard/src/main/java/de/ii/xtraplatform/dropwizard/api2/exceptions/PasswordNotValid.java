/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.api.exceptions;

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