package de.ii.xsf.core.firstrun.views;

import de.ii.xsf.core.api.firstrun.FirstRunPage;
import io.dropwizard.views.View;

/**
 *
 * @author fischer
 */
public class FirstRunView extends View {

    private FirstRunPage page;
    private String pageid;
    
    public FirstRunView() {
        super("firstrun.mustache");
    }

    public FirstRunPage getPage() {
        return page;
    }

    public void setPage(FirstRunPage page) {
        this.page = page;
    }

    public String getPageid() {
        return pageid;
    }

    public void setPageid(String pageid) {
        this.pageid = pageid;
    }
    
}
