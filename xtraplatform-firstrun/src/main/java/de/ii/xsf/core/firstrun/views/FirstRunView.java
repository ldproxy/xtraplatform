/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
