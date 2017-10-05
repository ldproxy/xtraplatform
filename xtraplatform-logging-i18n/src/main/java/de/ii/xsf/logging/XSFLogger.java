/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.logging;

import java.util.Locale;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.i18n.slf4j.LocalizedLoggerFactory;

/**
 *
 * @author fischer
 */
public class XSFLogger {
    private static LocalizedLoggerFactory loggerFactory = LocalizedLoggerFactory.getInstance(Locale.ROOT);
        
    public static void setLocale(Locale locale) {
        loggerFactory = LocalizedLoggerFactory.getInstance(locale);
    }
        
    public static LocalizedLogger getLogger(Class clazz){       
        return loggerFactory.getLocalizedLogger(clazz);
    }
}
