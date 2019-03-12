/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.api;

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
