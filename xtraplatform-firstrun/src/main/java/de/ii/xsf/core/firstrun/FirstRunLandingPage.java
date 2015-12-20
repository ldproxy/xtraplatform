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
