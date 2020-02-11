// @Checkstyle:off
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Portions Copyright 2014-2015 ForgeRock AS.
 * Copied from commons-lang:commons-lang:2.6 org.apache.commons.lang.reflect.ConstructUtils with un-required methods
 * removed.
 */
package org.forgerock.guice.core.internal.commons.lang.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * <p> Utility reflection methods focussed on constructors, modelled after {@link MethodUtils}. </p>
 *
 * <h3>Known Limitations</h3>
 * <h4>Accessing Public Constructors In A Default Access Superclass</h4>
 * <p>There is an issue when invoking public constructors contained in a default access superclass.
 * Reflection locates these constructors fine and correctly assigns them as public.
 * However, an <code>IllegalAccessException</code> is thrown if the constructors is invoked.</p>
 *
 * <p><code>ConstructorUtils</code> contains a workaround for this situation.
 * It will attempt to call <code>setAccessible</code> on this constructor.
 * If this call succeeds, then the method can be invoked as normal.
 * This call will only succeed when the application has sufficient security privilages.
 * If this call fails then a warning will be logged and the method may fail.</p>
 *
 * @author Apache Software Foundation
 * @author Craig R. McClanahan
 * @author Ralph Schaer
 * @author Chris Audley
 * @author Rey Francois
 * @author Gregor Rayman
 * @author Jan Sorensen
 * @author Robert Burrell Donkin
 * @author Rodney Waldhoff
 * @since 2.5
 * @version $Id: ConstructorUtils.java 905636 2010-02-02 14:03:32Z niallp $
 */
public final class ConstructorUtils {

    /**
     * <p>ConstructorUtils instances should NOT be constructed in standard programming.
     * Instead, the class should be used as
     * <code>ConstructorUtils.invokeConstructor(cls, args)</code>.</p>
     */
    private ConstructorUtils() {
    }

    /**
     * Returns a constructor given a class and signature.
     * @param cls the class to be constructed
     * @param parameterTypes the parameter array
     * @return null if matching accessible constructor can not be found
     * @see Class#getConstructor
     * @see #getAccessibleConstructor(java.lang.reflect.Constructor)
     */
    public static Constructor<?> getAccessibleConstructor(Class<?> cls, Class<?>[] parameterTypes) {
        try {
            return getAccessibleConstructor(cls.getConstructor(parameterTypes));
        } catch (NoSuchMethodException e) {
            return (null);
        }
    }

    /**
     * Returns accessible version of the given constructor.
     * @param ctor prototype constructor object.
     * @return <code>null</code> if accessible constructor can not be found.
     * @see java.lang.SecurityManager
     */
    public static Constructor<?> getAccessibleConstructor(Constructor<?> ctor) {
        return MemberUtils.isAccessible(ctor)
                && Modifier.isPublic(ctor.getDeclaringClass().getModifiers()) ? ctor : null;
    }
}
// @Checkstyle:on
