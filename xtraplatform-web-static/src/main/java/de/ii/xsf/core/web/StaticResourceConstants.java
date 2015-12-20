package de.ii.xsf.core.web;

/**
 *
 * @author zahnen
 */
public interface StaticResourceConstants {
    /** The indicator the for resources themselves. */
    String WEB_RESOURCE_KEY = "X-Web-Resource";

    /** The indicator that a bundle exposes additional web resources. */
    String WEB_RESOURCE_VERSION_KEY = "X-Web-Resource-Version";
    /** The baseline version for this web resources implementation. */
    String WEB_RESOURCE_BASELINE_VERSION = "1.0";
    /** The version in which we added support for {@link #WEB_RESOURCE_DEFAULT_PAGE}. */
    //Version WEB_RESOURCE_VERSION_1_1 = new Version("1.1");

    /** The (array of) string(s) that indicate the default pages to use for certain paths. */
    String WEB_RESOURCE_DEFAULT_PAGE = "X-Web-Resource-Default-Page";

    /** Defines a filter clause for bundles that want to register a resource using this bundle. */
    String WEB_RESOURCE_FILTER = String.format("(&(%s>=%s)(%s=*))", WEB_RESOURCE_VERSION_KEY,
        WEB_RESOURCE_BASELINE_VERSION, WEB_RESOURCE_KEY);

    /** Constant used to denote that adding caching headers is disabled. */
    long CACHE_TIMEOUT_DISABLED = -1L;
    /** Default cache timeout to use. */
    long ONE_WEEK_IN_SECONDS = 604800L;

    /** Key used for the contextId */
    String CONTEXTID = "ContextId";

    /** HTTP headers */
    String HTTP_CACHE_CONTROL = "Cache-Control";
    String HTTP_LAST_MODIFIED = "Last-Modified";
    String HTTP_IF_MODIFIED_SINCE = "If-Modified-Since";
}
