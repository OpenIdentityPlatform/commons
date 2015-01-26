/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.authz.filter.servlet;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.authz.filter.api.AuthorizationContext;
import org.forgerock.authz.filter.api.AuthorizationException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.servlet.api.HttpServletAuthorizationModule;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

public class AuthorizationModulesTest {

    @Test
    public void shouldCreateAuthorizationModuleFactory() {
        //Given
        FilterConfig config = mock(FilterConfig.class);
        when(config.getInitParameter(AuthorizationModules.INIT_PARAM_MODULE_CLASS_NAME)).thenReturn(MyModule.class.getName());

        //When
        AuthorizationModuleFactory moduleFactory = AuthorizationModules.getAuthorizationModuleFactory(config);

        //Then
        HttpServletAuthorizationModule authorizationModule = moduleFactory.getAuthorizationModule();
        assertThat(authorizationModule).isInstanceOf(MyModule.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldFailWithoutConfig() {
        //Given
        FilterConfig config = mock(FilterConfig.class);

        //When
        AuthorizationModules.getAuthorizationModuleFactory(config);

        //Then exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldFailIfWrongType() {
        //Given
        FilterConfig config = mock(FilterConfig.class);
        when(config.getInitParameter(AuthorizationModules.INIT_PARAM_MODULE_CLASS_NAME)).thenReturn(this.getClass().getName());

        //When
        AuthorizationModules.getAuthorizationModuleFactory(config);

        //Then exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldFailIfMissingType() {
        //Given
        FilterConfig config = mock(FilterConfig.class);
        when(config.getInitParameter(AuthorizationModules.INIT_PARAM_MODULE_CLASS_NAME)).thenReturn("fred");

        //When
        AuthorizationModules.getAuthorizationModuleFactory(config);

        //Then exception
    }

    public static class MyModule implements HttpServletAuthorizationModule {
        @Override
        public Promise<AuthorizationResult, AuthorizationException> authorize(HttpServletRequest req, AuthorizationContext context) {
            return null;
        }
    }
}
