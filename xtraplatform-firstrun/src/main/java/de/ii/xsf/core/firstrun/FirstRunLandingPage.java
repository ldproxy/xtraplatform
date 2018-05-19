/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.firstrun;

import de.ii.xsf.core.api.firstrun.FirstRunPage;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class FirstRunLandingPage implements FirstRunPage {
    
    @Override
    public String getTitle() {
        return "Welcome to <b>XtraProxy</b>";
    }

    @Override
    public String getDescription() {
        return "Click on <b>next</b> to start the initial configuration";
    }

    @Override
    public String getForm() {
        return null;
    }

    @Override
    public boolean needsConfig() {
        return false;
    }
    
    @Override
    public void setResult( Map<String,String[]> result){
        // nothing to do
    }
    
    @Override
    public boolean isFirstPage() {
        return false;
    }
    
    @Override
    public boolean configIsDone() {
        return false;
    }
}
