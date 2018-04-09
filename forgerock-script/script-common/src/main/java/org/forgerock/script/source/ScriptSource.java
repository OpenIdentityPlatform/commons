/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
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

import org.forgerock.script.ScriptName;

import java.io.IOException;
import java.io.Reader;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public interface ScriptSource extends SourceUnit {

    String guessType();

    /**
     * Returns a new Reader on the underlying source object. Returns null if the
     * source can't be reopened.
     *
     * @throws java.io.IOException
     *             if there was an error opening for stream
     * @return the reader to the resource
     */
    Reader getReader() throws IOException;

    /**
     *
     * @return
     */
    ScriptName[] getDependencies();
}
