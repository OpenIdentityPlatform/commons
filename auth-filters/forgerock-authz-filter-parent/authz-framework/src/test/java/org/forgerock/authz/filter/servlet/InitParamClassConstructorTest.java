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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class InitParamClassConstructorTest {

    private InitParamClassConstructor initParamClassConstructor;

    @BeforeMethod
    public void setUp() {
        initParamClassConstructor = new InitParamClassConstructor();
    }

    @Test
    public void constructShouldReturnNullWhenFactoryClassNameInitParamNotSet() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(null);

        //When
        String instance = initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam,
                defaultMethodName);

        //Then
        assertNull(instance);
    }

    @Test (expectedExceptions = ServletException.class)
    public void constructShouldThrowServletExceptionWhenClassCannotBeFound() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn("UnknownClass");

        //When
        initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam, defaultMethodName);

        //Then
        // Expected ServletException
    }

    @Test (expectedExceptions = ServletException.class)
    public void constructShouldThrowServletExceptionWhenDefaultMethodDoesNotExistOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(TestClassOne.class.getName());

        //When
        initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam, defaultMethodName);

        //Then
        // Expected ServletException
    }

    @Test
    public void constructShouldCallZeroArgNonDefaultFactoryMethod() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(TestClassOne.class.getName());
        given(config.getInitParameter(factoryMethodNameParam)).willReturn("nonDefaultMethodName");

        //When
        String instance = initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam,
                defaultMethodName);

        //Then
        assertEquals(instance, TestClassOne.class.getSimpleName());
    }

    @Test
    public void constructShouldCallZeroArgDefaultFactoryMethod() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(TestClassTwo.class.getName());

        //When
        String instance = initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam,
                defaultMethodName);

        //Then
        assertEquals(instance, TestClassTwo.class.getSimpleName());
    }

    @Test
    public void constructShouldCallFilterConfigArgNonDefaultFactoryMethod() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(TestClassThree.class.getName());
        given(config.getInitParameter(factoryMethodNameParam)).willReturn("nonDefaultMethodName");

        //When
        String instance = initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam,
                defaultMethodName);

        //Then
        assertEquals(instance, TestClassThree.class.getSimpleName());
    }

    @Test
    public void constructShouldCallFilterConfigArgDefaultFactoryMethod() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String factoryClassParam = "FACTORY_CLASS_PARAM";
        String factoryMethodNameParam = "FACTORY_METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        given(config.getInitParameter(factoryClassParam)).willReturn(TestClassFour.class.getName());

        //When
        String instance = initParamClassConstructor.construct(config, factoryClassParam, factoryMethodNameParam,
                defaultMethodName);

        //Then
        assertEquals(instance, TestClassFour.class.getSimpleName());
    }

    private static final class TestClassOne {
        public static String nonDefaultMethodName() {
            return TestClassOne.class.getSimpleName();
        }
    }

    private static final class TestClassTwo {
        public static String defaultMethodName() {
            return TestClassTwo.class.getSimpleName();
        }
    }

    private static final class TestClassThree {
        public static String nonDefaultMethodName(FilterConfig config) {
            return TestClassThree.class.getSimpleName();
        }
    }

    private static final class TestClassFour {
        public static String defaultMethodName(FilterConfig config) {
            return TestClassFour.class.getSimpleName();
        }
    }
}
