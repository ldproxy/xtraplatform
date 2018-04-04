/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.api.exceptions;

import com.fasterxml.jackson.databind.util.JSONPObject;
import de.ii.xsf.core.api.exceptions.XtraserverFrameworkException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author fischer
 */
public class ArcGisTokenRequiredException extends XtraserverFrameworkException {

    public ArcGisTokenRequiredException(Object m, Object... args) {
        super(m, args);
        this.init();
    }
    
    public ArcGisTokenRequiredException() {
        this.init();
    }
    
    private void init() {
        this.code = Response.Status.UNAUTHORIZED;
        this.htmlCode = this.code;             
        this.stdmsg = "Token Required";
    }
    
    @Override
    public int getCode() {
        return 499;
    }
    
    @Override
    public int getHtmlCode() {
        return 499;
    }
    
    public ArcGisTokenRequiredException(String msg) {
        this();
        this.msg = msg;
    }
    
    public ArcGisTokenRequiredException(String msg, String callback) {
        this();
        this.msg = msg;
        this.callback = callback;
    }
    
    @Override
    public Response getResponse() {

        JsonError error = new JsonError(getCode(), stdmsg);
        error.addDetail(msg);

        for (String detail : this.details) {
            error.addDetail(detail);
        }

        if (callback != null && !callback.isEmpty()) {

            return Response.ok(new JSONPObject(this.callback, error), MediaType.APPLICATION_JSON).build();
        }
        return Response.ok(error, MediaType.APPLICATION_JSON).build();
    }
}