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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.forgerock.json.jose.utils.KeystoreManager;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class KeystoreManagerTest {

    @Test
    public void should() throws UnsupportedEncodingException {

        KeystoreManager keystoreManager = new KeystoreManager("JKS",
                URLDecoder.decode(ClassLoader.getSystemResource("keystore.jks").getFile(), "UTF-8"), "password");

        keystoreManager.getPrivateKey("jwt-test-ks", "password");
    }
}
