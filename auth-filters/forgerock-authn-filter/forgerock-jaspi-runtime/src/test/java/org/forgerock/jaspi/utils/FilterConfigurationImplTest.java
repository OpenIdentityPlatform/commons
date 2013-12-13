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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.utils;

import org.testng.annotations.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class FilterConfigurationImplTest {

    @Test
    public void shouldReturnNullWhenFilterConfigIsNull() throws ServletException {

        //Given
        FilterConfig config = null;
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertNull(obj);
    }

    @Test
    public void shouldReturnNullWhenClassNameParamNotSet() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertNull(obj);
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldThrowServletExceptionWhenClassNotFound() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = "CLASS_NOT_FOUND";

        given(config.getInitParameter(classNameParam)).willReturn(className);

        //When
        FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldServletExceptionWhenMethodNameParamNotSetAndDefaultMethodNotOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestOne.class.getName();

        given(config.getInitParameter(classNameParam)).willReturn(className);

        //When
        FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        fail();
    }

    @Test (expectedExceptions = ServletException.class)
    public void shouldServletExceptionWhenMethodNameParamSetButNotOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestOne.class.getName();
        String methodName = "METHOD_NOT_ON_CLASS";

        given(config.getInitParameter(classNameParam)).willReturn(className);
        given(config.getInitParameter(methodNameParam)).willReturn(methodName);

        //When
        FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        fail();
    }

    @Test
    public void shouldGetInstanceWhenMethodNameParamNotSetAndNoArgsDefaultMethodOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestTwo.class.getName();

        given(config.getInitParameter(classNameParam)).willReturn(className);

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertTrue(obj.getClass().isAssignableFrom(FilterConfigurationTestTwo.class));
    }

    @Test
    public void shouldServletExceptionWhenMethodNameParamSetAndNoArgsMethodOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestThree.class.getName();
        String methodName = "customMethodName";

        given(config.getInitParameter(classNameParam)).willReturn(className);
        given(config.getInitParameter(methodNameParam)).willReturn(methodName);

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertTrue(obj.getClass().isAssignableFrom(FilterConfigurationTestThree.class));
    }

    @Test
    public void shouldGetInstanceWhenMethodNameParamNotSetAndArgsDefaultMethodOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestTwo.class.getName();

        given(config.getInitParameter(classNameParam)).willReturn(className);

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertTrue(obj.getClass().isAssignableFrom(FilterConfigurationTestTwo.class));
    }

    @Test
    public void shouldServletExceptionWhenMethodNameParamSetAndArgsMethodOnClass() throws ServletException {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        String classNameParam = "CLASS_NAME_PARAM";
        String methodNameParam = "METHOD_NAME_PARAM";
        String defaultMethodName = "defaultMethodName";

        String className = FilterConfigurationTestThree.class.getName();
        String methodName = "customMethodName";

        given(config.getInitParameter(classNameParam)).willReturn(className);
        given(config.getInitParameter(methodNameParam)).willReturn(methodName);

        //When
        Object obj = FilterConfigurationImpl.INSTANCE.get(config, classNameParam, methodNameParam, defaultMethodName);

        //Then
        assertTrue(obj.getClass().isAssignableFrom(FilterConfigurationTestThree.class));
    }

    static class FilterConfigurationTestOne {

    }

    static class FilterConfigurationTestTwo {
        public static Object defaultMethodName() {
            return new Object();
        }
    }

    static class FilterConfigurationTestThree {
        public static Object customMethodName() {
            return new Object();
        }
    }

    static class FilterConfigurationTestFour {
        public static Object defaultMethodName(FilterConfig config) {
            return new Object();
        }
    }

    static class FilterConfigurationTestFive {
        public static Object customMethodName(FilterConfig config) {
            return new Object();
        }
    }
}
