package de.ii.xsf.core.api.organization;

/**
 *
 * @author fischer
 */
public class OrganizationDecider {
    public static String MULTI_TENANCY_ROOT = "_multi_tenancy_root_";
    
    public static boolean isRootOrg(String orgid) {
        return orgid == null || orgid.equals(MULTI_TENANCY_ROOT);
    }

    public static boolean isMultiTenancyRootOrg(String orgid) {
        return MULTI_TENANCY_ROOT.equals(orgid);
    }
}
