package de.ii.xsf.core.api.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
public class Error {
    private int code;
    private String message;
    private List<String> details;

    public Error( int code, String message){ 
        this.code = code;
        this.message = message;
        this.details = new ArrayList();
    }
    
    public void addDetail(String detail){
        this.details.add(detail);
    }
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
    
    
}
