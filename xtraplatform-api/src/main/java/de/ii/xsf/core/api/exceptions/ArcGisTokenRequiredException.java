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