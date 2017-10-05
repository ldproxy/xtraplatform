/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.core.rest.auth;

import de.ii.xsf.core.api.permission.Auth;
import de.ii.xsf.core.api.permission.AuthenticatedUser;
import de.ii.xsf.core.api.permission.Organization;
import de.ii.xsf.core.api.permission.Role;
import javax.servlet.http.HttpServletRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

/**
 *
 * @author zahnen
 */
public class NoOpAuthProviderTest {
    private static final String TEST_ORG = "org1";
    private SoftAssert ass = new SoftAssert();

    @InjectMocks
    private NoOpAuthProvider provider;
 
    @Mock
    private HttpServletRequest request;

    @org.testng.annotations.BeforeClass(groups = {"default"})
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @org.testng.annotations.Test(groups = {"default"})
    public void testInjectionWithOrganization() {
        // return organization attribute from HttpServletRequest
        Mockito.when(request.getAttribute(Organization.class.getName())).thenReturn(TEST_ORG);
        
        AuthenticatedUser u = provider.getInjectable(null, null, null).getValue(null);
        
        Assert.assertEquals(u.getOrgId(), TEST_ORG);
        Assert.assertEquals(u.getRole(), Role.NONE);
        
    }
    
    @org.testng.annotations.Test(groups = {"default"})
    public void testInjectionWithoutOrganization() {
        // return null, happens when attribute does not exist in HttpServletRequest
        Mockito.when(request.getAttribute(Organization.class.getName())).thenReturn(null);
        
        AuthenticatedUser u = provider.getInjectable(null, null, null).getValue(null);
        
        Assert.assertEquals(u.getOrgId(), null);
        Assert.assertEquals(u.getRole(), Role.NONE);
                
    }
    
    @org.testng.annotations.Test(groups = {"default"})
    public void testInjectionWithRequired() {
        // return null, happens when attribute does not exist in HttpServletRequest
        Mockito.when(request.getAttribute(Organization.class.getName())).thenReturn(null);
        
        // set required to true
        Auth a = mock(Auth.class);
        when(a.required()).thenReturn(true);

        AuthenticatedUser u = provider.getInjectable(null, a, null).getValue(null);
        
        Assert.assertEquals(u.getOrgId(), null);
        Assert.assertEquals(u.getRole(), Role.NONE);
    }
    
    @org.testng.annotations.AfterClass(groups = {"default"})
    public void cleanup() {
        
    }

}
