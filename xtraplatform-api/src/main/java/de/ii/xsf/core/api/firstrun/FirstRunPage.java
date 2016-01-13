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
