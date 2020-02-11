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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.util.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.testng.annotations.Test;

public class IdGeneratorTest {

    @Test
    public void shouldGenerateIdWIthUuidAsPrefix() {
        final String expectedPrefix = "064daa77-f856-492b-bcb4-e26c10d748c1";
        IdGenerator.SequenceUuidIdGenerator generator = new IdGenerator.SequenceUuidIdGenerator(UUID.fromString(
                expectedPrefix));

        String id1 = generator.generate();
        String id2 = generator.generate();

        assertThat(id1).isEqualTo(expectedPrefix + '-' + 0);
        assertThat(id2).isEqualTo(expectedPrefix + '-' + 1);
    }

}
