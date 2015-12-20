package de.ii.xsf.core.api.exceptions;

/**
 *
 * @author fischer
 */
public class JsonError {

    private Error error;

    public JsonError(int code, String msg) {   
        this.error = new Error(code, msg);
    }
    
    public void addDetail(String detail){
        this.error.addDetail(detail);
    }
 
    public Error getError() {
        return this.error;
    }

}
