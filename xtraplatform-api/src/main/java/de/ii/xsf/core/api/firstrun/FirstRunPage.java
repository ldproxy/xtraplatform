package de.ii.xsf.core.api.firstrun;

import java.io.IOException;
import java.util.Map;

/**
 * A FirstRunPage is used at first startup of the application to collect some
 * information from the user.
 *
 * This interface is used by {@link FirstrunPageRegistry} where an
 * implementation of this interface is registered.
 *
 * A FirstRunPage implementation provides the substitutions needed in
 * {@link firstrun.mustache}
 *
 * @author fischer
 */
public interface FirstRunPage {

    /**
     *
     * @return the title for the page
     */
    String getTitle();

    /**
     *
     * @return the description for the page
     */
    String getDescription();

    /**
     *
     * @return returns the form for the values needed by the implementation
     */
    String getForm();

    /**
     *
     * @return true if the values needed are not collected
     */
    boolean needsConfig();

    /**
     *
     * @param result the result of the users input collected in the form
     * @throws java.io.IOException
     */
    void setResult(Map<String, String[]> result) throws IOException;
    
    boolean isFirstPage();
    
    boolean configIsDone();
}
