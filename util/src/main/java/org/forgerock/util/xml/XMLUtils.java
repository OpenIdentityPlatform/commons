/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XMLUtils.java,v 1.15 2009/10/19 18:19:20 asyhuang Exp $
 *
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */
package org.forgerock.util.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLUtils {

    /**
     * Provides a secure DocumentBuilder implementation, which is protected against
     * different types of entity expansion attacks and makes sure that only locally
     * available DTDs can be referenced within the XML document.
     * @param validating Whether the returned DocumentBuilder should validate input.
     * @return A secure DocumentBuilder instance.
     * @throws javax.xml.parsers.ParserConfigurationException In case xerces does not support one
     * of the required features.
     */
    public static DocumentBuilder getSafeDocumentBuilder(boolean validating) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(validating);
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(false);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new XMLHandler());
        return db;
    }
}
