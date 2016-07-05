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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.jose.jwe;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EncryptionMethodTest {

    @DataProvider
    private Object[][] specCompliantNames() {
        return new Object[][]{
            {EncryptionMethod.A128CBC_HS256, "A128CBC-HS256"},
            {EncryptionMethod.A192CBC_HS384, "A192CBC-HS384"},
            {EncryptionMethod.A256CBC_HS512, "A256CBC-HS512"},
            {EncryptionMethod.A128GCM, "A128GCM"},
            {EncryptionMethod.A192GCM, "A192GCM"},
            {EncryptionMethod.A256GCM, "A256GCM"}
        };
    }

    @Test(dataProvider = "specCompliantNames")
    public void toStringShouldReturnSpecCompliantName(EncryptionMethod encryptionMethod, String specCompliantName) {
        assertThat(encryptionMethod.toString()).isEqualTo(specCompliantName);
    }

    @Test(dataProvider = "specCompliantNames")
    public void parseShouldAcceptSpecCompliantName(EncryptionMethod encryptionMethod, String specCompliantName) {
        assertThat(EncryptionMethod.parseMethod(specCompliantName)).isEqualTo(encryptionMethod);
    }

    @Test(dataProvider = "specCompliantNames")
    public void parseShouldAcceptBackwardCompatibleEncryptionMethods(EncryptionMethod method, String ignored) {
        assertThat(EncryptionMethod.parseMethod(method.name())).isEqualTo(method);
    }
}
