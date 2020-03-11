/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.source;

import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;

import java.net.URI;
import java.net.URL;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface SourceUnit {

    public static final String AUTO_DETECT = "auto-detect";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_REVISION = "revision";
    public static final String ATTR_SOURCE = "source";
    public static final String ATTR_VISIBILITY = "visibility";
    public static final String ATTR_REQUEST_BINDING = "request-binding";

    ScriptName getName();

    URL getSource();

    URI getSourceURI();

    ScriptEntry.Visibility getVisibility();

    /**
     * Get the parent container if there is one.
     *
     * @return the parent container where this instance belongs to, or
     *         <tt>null</tt> if there is no parent.
     */
    SourceContainer getParentContainer();
}
