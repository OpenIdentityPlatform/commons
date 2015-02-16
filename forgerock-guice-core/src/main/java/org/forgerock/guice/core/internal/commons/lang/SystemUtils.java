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
package org.forgerock.guice.core.internal.commons.lang;

/**
 * <p>Helpers for <code>java.lang.System</code>.</p>
 *
 * <p>If a system property cannot be read due to security restrictions,
 * the corresponding field in this class will be set to <code>null</code>
 * and a message will be written to <code>System.err</code>.</p>
 *
 * @author Apache Software Foundation
 * @author Based on code from Avalon Excalibur
 * @author Based on code from Lucene
 * @author <a href="mailto:sdowney@panix.com">Steve Downey</a>
 * @author Gary Gregory
 * @author Michael Becke
 * @author Tetsuya Kaneuchi
 * @author Rafal Krupinski
 * @author Jason Gritman
 * @since 1.0
 * @version $Id: SystemUtils.java 905707 2010-02-02 16:59:59Z niallp $
 */
public final class SystemUtils {

    /**
     * <p>The <code>java.version</code> System Property. Java version number.</p>
     *
     * <p>Defaults to <code>null</code> if the runtime does not have
     * security access to read this property or the property does not exist.</p>
     *
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)}
     * or {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value
     * will be out of sync with that System property.
     * </p>
     *
     * @since Java 1.1
     */
    public static final String JAVA_VERSION = getSystemProperty("java.version");

    /**
     * <p>Gets the Java version as a <code>String</code> trimming leading letters.</p>
     *
     * <p>The field will return <code>null</code> if {@link #JAVA_VERSION} is <code>null</code>.</p>
     *
     * @since 2.1
     */
    public static final String JAVA_VERSION_TRIMMED = getJavaVersionTrimmed();

    /**
     * <p>Gets the Java version as a <code>float</code>.</p>
     *
     * <p>Example return values:</p>
     * <ul>
     *  <li><code>1.2f</code> for JDK 1.2
     *  <li><code>1.31f</code> for JDK 1.3.1
     * </ul>
     *
     * <p>The field will return zero if {@link #JAVA_VERSION} is <code>null</code>.</p>
     *
     * @since 2.0
     */
    public static final float JAVA_VERSION_FLOAT = getJavaVersionAsFloat();

    /**
     * <p>SystemUtils instances should NOT be constructed in standard
     * programming. Instead, the class should be used as
     * <code>SystemUtils.FILE_SEPARATOR</code>.</p>
     */
    private SystemUtils() {
    }

    /**
     * <p>Gets the Java version number as a <code>float</code>.</p>
     *
     * <p>Example return values:</p>
     * <ul>
     *  <li><code>1.2f</code> for JDK 1.2
     *  <li><code>1.31f</code> for JDK 1.3.1
     * </ul>
     *
     * <p>Patch releases are not reported.
     * Zero is returned if {@link #JAVA_VERSION_TRIMMED} is <code>null</code>.</p>
     *
     * @return the version, for example 1.31f for JDK 1.3.1
     */
    private static float getJavaVersionAsFloat() {
        if (JAVA_VERSION_TRIMMED == null) {
            return 0f;
        }
        String str = JAVA_VERSION_TRIMMED.substring(0, 3);
        if (JAVA_VERSION_TRIMMED.length() >= 5) {
            str = str + JAVA_VERSION_TRIMMED.substring(4, 5);
        }
        try {
            return Float.parseFloat(str);
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Trims the text of the java version to start with numbers.
     *
     * @return the trimmed java version
     */
    private static String getJavaVersionTrimmed() {
        if (JAVA_VERSION != null) {
            for (int i = 0; i < JAVA_VERSION.length(); i++) {
                char ch = JAVA_VERSION.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    return JAVA_VERSION.substring(i);
                }
            }
        }
        return null;
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Gets a System property, defaulting to <code>null</code> if the property
     * cannot be read.</p>
     *
     * <p>If a <code>SecurityException</code> is caught, the return
     * value is <code>null</code> and a message is written to <code>System.err</code>.</p>
     *
     * @param property the system property name
     * @return the system property value or <code>null</code> if a security problem occurs
     */
    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            System.err.println(
                "Caught a SecurityException reading the system property '" + property
                + "'; the SystemUtils property value will default to null."
            );
            return null;
        }
    }

    /**
     * <p>Is the Java version at least the requested version.</p>
     *
     * <p>Example input:</p>
     * <ul>
     *  <li><code>1.2f</code> to test for JDK 1.2</li>
     *  <li><code>1.31f</code> to test for JDK 1.3.1</li>
     * </ul>
     *
     * @param requiredVersion  the required version, for example 1.31f
     * @return <code>true</code> if the actual version is equal or greater
     *  than the required version
     */
    public static boolean isJavaVersionAtLeast(float requiredVersion) {
        return JAVA_VERSION_FLOAT >= requiredVersion;
    }
}
// @Checkstyle:on
