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
package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class StatusTest {

    @Test
    public void shouldBuildUnknownValidStatusCode() throws Exception {
        Status status = Status.valueOf(107);
        assertThat(status.getCode()).isEqualTo(107);
        assertThat(status.getReasonPhrase()).isEqualTo("Unknown");
        assertThat(status.getFamily()).isEqualTo(Status.Family.INFORMATIONAL);
    }

    @Test
    public void shouldCreateNewStatusAndCacheIt() throws Exception {
        Status status = Status.valueOf(607);
        assertThat(status.getReasonPhrase()).isEqualTo("Unknown");
        assertThat(status.getCode()).isEqualTo(607);

        assertThat(Status.valueOf(607)).isSameAs(status);
    }

    @Test
    public void shouldCreateNewStatusWithReasonPhraseAndCacheIt() throws Exception {
        Status status = Status.valueOf(777, "My reason phrase");
        assertThat(status.getReasonPhrase()).isEqualTo("My reason phrase");
        assertThat(status.getCode()).isEqualTo(777);

        assertThat(Status.valueOf(777)).isSameAs(status);
    }

    @Test
    public void shouldConstructStatus() throws Exception {
        Status status = Status.valueOf(222, "Blah");
        assertThat(status.getFamily()).isEqualTo(Status.Family.SUCCESSFUL);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void shouldNotConstructStatusLessThan100() throws Exception {
        Status.valueOf(10, "Blah");
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void shouldNotConstructStatusGreaterThan999() throws Exception {
        Status.valueOf(1000, "Blah");
    }

    @Test
    public void shouldLookupFamily() throws Exception {
        Status.Family family = Status.Family.valueOf(200);
        assertThat(family).isEqualTo(Status.Family.SUCCESSFUL);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void shouldNotLookupFamilyWithInvalidCode() throws Exception {
        Status.Family.valueOf(10);
    }
}
