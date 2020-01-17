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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.doc.maven.utils;

import org.forgerock.doc.maven.utils.helper.FileFilterFactory;

/**
 * Update XML files based on an XSL resource.
 */
public class KeepTogetherTransformer extends XmlTransformer {

    /**
     * Construct an updater to match DocBook XML files.
     *
     * <p>
     *
     * The files are updated in place.
     *
     * <p>
     *
     * The following example shows how this might be used in your code.
     *
     * <pre>
     *     File xmlSourceDirectory  = new File("/path/to/xml/files/");
     *
     *     // Update XML files.
     *     KeepTogetherTransformer xslt = new KeepTogetherTransformer();
     *     return xslt.update(xmlSourceDirectory);
     * </pre>
     */
    public KeepTogetherTransformer() {
        super(FileFilterFactory.getXmlFileFilter(), "/xslt/keep-together.xsl");
    }
}
