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
