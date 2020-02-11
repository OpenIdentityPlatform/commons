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
package org.forgerock.selfservice.stages.utils;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;

import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JsonUtils}.
 *
 * @since 0.2.0
 */
public final class JsonUtilsTest {

    @Test
    public void testMerge() throws Exception {
        // Given
        JsonValue json1 = json(object(
                field("key1", 0),
                field("key2", 2)));
        JsonValue json2 = json(object(
                field("key3", 3),
                field("key4", 4)));
        JsonValue json3 = json(object(
                field("key1", 1)));

        // When
        JsonValue jsonFinal = JsonUtils.merge(json1, json2, json3, null);

        // Then
        assertThat(jsonFinal).integerAt("key1").isNotEqualTo(0);

        assertThat(jsonFinal).integerAt("key1").isEqualTo(1);
        assertThat(jsonFinal).integerAt("key2").isEqualTo(2);
        assertThat(jsonFinal).integerAt("key3").isEqualTo(3);
        assertThat(jsonFinal).integerAt("key4").isEqualTo(4);

    }

    @Test
    public void testMergeNullInput() throws Exception {
        // Given
        JsonValue json = null;

        // When
        JsonValue jsonFinal = JsonUtils.merge(json);

        // Then
        assertThat(jsonFinal).isEmpty();
    }
}
