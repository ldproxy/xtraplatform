/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.firstrun;

import de.ii.xsf.core.api.firstrun.FirstRunPage;
import de.ii.xsf.core.api.permission.Organization;
import de.ii.xsf.core.firstrun.views.FirstRunView;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.logging.XSFLogger;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import org.apache.felix.ipojo.annotations.*;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zahnen
 */
@Component
@Provides(properties = {
    @StaticServiceProperty(name = "alias", type = "java.lang.String", value = "/")
})
@Instantiate

public class FirstRunServlet extends HttpServlet {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FirstRunServlet.class);
    private static final String MANAGER_PATH = "manager/";

    @Requires
    FirstRunPageRegistry reg;

    private final MustacheViewRenderer renderer;
    private final FirstRunView view;
    private final String externalPath;
    private final List<FirstRunPage> pagesEnabled;

    public FirstRunServlet(@Requires Dropwizard server) {
        this.renderer = new MustacheViewRenderer();
        this.view = new FirstRunView();
        this.pagesEnabled = new ArrayList<>();

        // determine path fragment of external URL
        String extPath = "";
        if (server.hasExternalUrl()) {
            String extUrl = server.getExternalUrl();
            extPath = extUrl.substring(extUrl.indexOf("/", extUrl.indexOf("//") + 2) + 1);

            if (!extPath.isEmpty() && !extPath.endsWith("/")) {
                extPath += "/";
            }
        }
        this.externalPath = extPath;
    }

    private boolean isRootPath(String path) {
        return path == null || path.equals("/") || path.equals(externalPath);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (!isRootPath(request.getPathInfo())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean needsConfig = false;
        pagesEnabled.clear();
        for (FirstRunPage p : reg.getPages()) {
            if (p.needsConfig()) {
                // put a forced firstPage at index 0 in the list
                if (p.isFirstPage()) {
                    pagesEnabled.add(0, p);

                } else {
                    pagesEnabled.add(p);
                }
                needsConfig = true;
            }
        }

        if (!needsConfig) {
            this.doRedirectToManager(request, response);
        } else {
            view.setPageid("0");
            view.setPage(new FirstRunLandingPage());
            this.doResponse(response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (!isRootPath(request.getPathInfo())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // we have to do this for ArcMap
        if (request.getParameter("PAGEID") == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        int pageId = Integer.parseInt(request.getParameter("PAGEID"));
        int pageCount = this.pagesEnabled.size();

        LOGGER.getLogger().info("FIRSTRUN: {} {}", pageId, pageCount);

        // handle previous input
        if (pageId - 1 < pageCount && pageId > 0) {

            FirstRunPage frp = this.pagesEnabled.get(pageId - 1);

            LOGGER.getLogger().info("FIRSTRUN: {} {}", frp.getTitle(), request.getParameterMap());
            try {
                frp.setResult(request.getParameterMap());
            } catch (Exception ex) {
                LOGGER.getLogger().error("FIRSTRUN ERROR: {} {}", frp.getTitle(), request.getParameterMap(), ex);

                // TODO: set error page as view
            }
            if (frp.configIsDone()) {
                pageId = pageCount; // we are done
                this.pagesEnabled.clear();
            }
        }

        if (pageId < pageCount) {

            FirstRunPage p = this.pagesEnabled.get(pageId);
            view.setPage(p);
            view.setPageid(String.valueOf(pageId + 1));

        } else if (pageId == pageCount) {
            view.setPage(new FirstRunDonePage());
            view.setPageid(String.valueOf(pageId + 1));
        } else if (pageId > pageCount) {

            this.doRedirectToManager(request, response);
            return;
        }

        this.doResponse(response);

    }

    private void doRedirectToManager(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String org = (String) request.getAttribute(Organization.class.getName());
        String path = MANAGER_PATH;
        if (org != null) {
            path = org + "/" + path;
        }
        path = externalPath + path;

        response.sendRedirect(path);
    }

    private void doResponse(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf8");
        renderer.render(view, null, response.getOutputStream());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }
}
