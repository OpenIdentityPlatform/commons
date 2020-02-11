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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.util.encode;

import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

@SuppressWarnings("javadoc")
public class Base64urlTest {

    @Test
    public void shouldBase64urlEncode() {

        //Given
        byte[] content = "43uin 98e2 + 343_ {} 43qafdgfREER\\'FDj ionk/.,<>`fj iod Hdfjl"
                .getBytes(Charset.forName("UTF-8"));
        String base64EncodedString = Base64.encode(content);

        //When
        String encodedString = Base64url.encode(content);

        //Then
        assertNotSame(encodedString, base64EncodedString);
        assertEquals(encodedString.split("-").length, base64EncodedString.split("\\+").length);
        assertEquals(encodedString.split("_").length, base64EncodedString.split("/").length);
        assertEquals(encodedString.indexOf("="), -1);
    }

    @Test
    public void shouldBase54urlDecode() {

        //Given
        String content1 = "NDN1aW4gOThlMiArIDM0M18ge30gNDNxYWZkZ2ZSRUVSXCdGRGogaW9uay8uLDw-YGZqIGlvZCBIZGZqbA";
        String content2 = "NDN1aW4gOThlMiArIDM0M18ge30gNDNxYWZkZ2ZSRUVSXCdGRGogaW9uay8uLDw-YGZqIGlvZCBIZGZqbGE";

        //When
        byte[] decodedBytes1 = Base64url.decode(content1);
        byte[] decodedBytes2 = Base64url.decode(content2);

        //Then
        assertEquals(new String(decodedBytes1, Charset.forName("UTF-8")),
                "43uin 98e2 + 343_ {} 43qafdgfREER\\'FDj ionk/.,<>`fj iod Hdfjl");
        assertEquals(new String(decodedBytes2, Charset.forName("UTF-8")),
                "43uin 98e2 + 343_ {} 43qafdgfREER\\'FDj ionk/.,<>`fj iod Hdfjla");
    }
}
