package de.ii.xsf.core.api;

/**
 *
 * @author zahnen
 */
public class Notification {

    public enum LEVEL {

        ERROR,
        WARNING,
        INFO
    }
    private LEVEL level;
    private String message;

    public Notification(){
        
    }
    
    public Notification(LEVEL level, String message) {
        this.level = level;
        this.message = message;
    }

    public LEVEL getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public void setLevel(LEVEL level) {
        this.level = level;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
