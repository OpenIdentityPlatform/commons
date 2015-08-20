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

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.http.routing.Version;
import org.forgerock.util.Pair;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AcceptApiVersionHeaderTest {

    @DataProvider
    private Object[][] parseData() {
        return new Object[][]{
            {"protocol=1.0,resource=2.0"},
            {"protocol=1.0, resource=2.0"},
            {"resource=1.0, protocol=2.0"},
        };
    }

    @Test(dataProvider = "parseData")
    public void shouldParse(String versionString) {

        //When
        Pair<Version, Version> version = AcceptApiVersionHeader.parse(versionString);

        //Then
        assertThat(version.getFirst()).isEqualTo(Version.version(1));
        assertThat(version.getSecond()).isEqualTo(Version.version(2));
    }
}
