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
import static org.forgerock.http.routing.Version.version;

import org.forgerock.http.routing.Version;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AcceptApiVersionHeaderTest {

    @DataProvider
    private Object[][] acceptedValues() {
        // @Checkstyle:off
        return new Object[][] {
            { "protocol=1.0,resource=2.0",  version(1),    version(2) },
            { "protocol=1,resource=2",      version(1),    version(2) },
            { "protocol=1.1, resource=2.4", version(1, 1), version(2, 4) },
            { "resource=1.0, protocol=2.0", version(2),    version(1) },
            { "resource=1.0",               null,          version(1) },
            { "",                           null,          null  },
            { null,                         null,          null  },
        };
        // @Checkstyle:on
    }

    @Test(dataProvider = "acceptedValues")
    public void shouldParse(String versionString, Version protocol, Version resource) {

        //When
        AcceptApiVersionHeader header = AcceptApiVersionHeader.valueOf(versionString);

        //Then
        assertThat(header.getProtocolVersion()).isEqualTo(protocol);
        assertThat(header.getResourceVersion()).isEqualTo(resource);
    }

    @DataProvider
    private Object[][] invalidValues() {
        // @Checkstyle:off
        return new Object[][] {
                { "PROTOCOL=1.0, RESOURCE=2.0" },
                { "protocol=2.0 resource=1.0" },
                { "protocol=2.0, resource=1.0, blah=3.0" },
                { "protocol=, resource=" },
                { "protocol=2.0.0, resource=1.0.0" },
                { "protocol=a, resource=b" },
                };
        // @Checkstyle:on
    }

    @Test(dataProvider = "invalidValues", expectedExceptions = IllegalArgumentException.class)
    public void shouldFailParsing(String versionString) {
        AcceptApiVersionHeader.valueOf(versionString);
    }
}
