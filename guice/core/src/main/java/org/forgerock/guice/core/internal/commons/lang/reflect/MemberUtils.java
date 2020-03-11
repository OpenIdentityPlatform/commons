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

import org.forgerock.guice.core.internal.commons.lang.SystemUtils;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Contains common code for working with Methods/Constructors, extracted and
 * refactored from <code>MethodUtils</code> when it was imported from Commons BeanUtils.
 *
 * @author Apache Software Foundation
 * @author Steve Cohen
 * @author Matt Benson
 * @since 2.5
 * @version $Id: MemberUtils.java 905636 2010-02-02 14:03:32Z niallp $
 */
final class MemberUtils {
    private static final Method IS_SYNTHETIC;

    static {
        Method isSynthetic = null;
        if (SystemUtils.isJavaVersionAtLeast(1.5f)) {
            // cannot call synthetic methods:
            try {
                @SuppressWarnings("rawtypes")
                Class[] c = new Class[0];
                isSynthetic = Member.class.getMethod("isSynthetic", c);
            } catch (Exception e) {
                //Empty Catch block
            }
        }
        IS_SYNTHETIC = isSynthetic;
    }

    /**
     * <p>MemberUtils instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * <code>MemberUtils.isAccessible</code>.</p>
     */
    private MemberUtils() {
    }

    /**
     * Check a Member for basic accessibility.
     * @param m Member to check
     * @return true if <code>m</code> is accessible
     */
    static boolean isAccessible(Member m) {
        return m != null && Modifier.isPublic(m.getModifiers()) && !isSynthetic(m);
    }

    /**
     * Try to learn whether a given member, on JDK >= 1.5, is synthetic.
     * @param m Member to check
     * @return true if <code>m</code> was introduced by the compiler.
     */
    static boolean isSynthetic(Member m) {
        if (IS_SYNTHETIC != null) {
            try {
                return ((Boolean) IS_SYNTHETIC.invoke(m)).booleanValue();
            } catch (Exception e) {
                //Empty Catch block
            }
        }
        return false;
    }
}
// @Checkstyle:on
