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

package org.forgerock.json.test.assertj;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.Conditions.*;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

public class AssertJJsonValueAssertTest {

    @Test
    public void testAssertThat() throws Exception {
        // Given
        JsonValue value = json(object(
                field("bool", true),
                field("int", 5),
                field("long", 3_257_259_826_582_038L),
                field("double", 50.91D),
                field("string", "fred"),
                field("null", null),
                field("obj", object(
                        field("subobj", object(
                                field("property", "abc")
                        )),
                        field("array", array(
                                5,
                                10,
                                "str",
                                null,
                                "str2"
                        ))
                )),
                field("emptyObject", object()),
                field("emptyArray", array())));

        // When
        AssertJJsonValueAssert.AbstractJsonValueAssert asserter = AssertJJsonValueAssert.assertThat(value);

        // Then
        asserter.isObject()
                .containsField("obj")
                .containsFields("bool", "double", "null")
                .contains("bool", true)
                .contains(Assertions.entry("int", 5), Assertions.entry("string", "fred"))
                .doesNotContain("int", 10)
                .doesNotContain(Assertions.entry("fred", 5), Assertions.entry("null", new Object()))
                .hasNull("null")
                .hasString("string")
                .hasNumber("int")
                .hasNumber("long")
                .hasNumber("double")
                .hasBoolean("bool")
                .hasString("obj/subobj/property");
        asserter.isObject().stringAt("string").isEqualTo("fred");
        asserter.isObject().booleanAt("bool").isTrue();
        asserter.isObject().integerAt("int").isEqualTo(5);
        asserter.isObject().longAt("long").isEqualTo(3_257_259_826_582_038L);
        asserter.isObject().doubleAt("double").isEqualTo(50.91D);
        asserter.isObject()
                .stringIs("string", equalTo("fred"))
                .integerIs("int", equalTo(5))
                .longIs("long", equalTo(3_257_259_826_582_038L))
                .doubleIs("double", equalTo(50.91D))
                .booleanIs("bool", equalTo(true));

        asserter.hasObject("obj/subobj")
                .containsOnly(Assertions.entry("property", "abc"))
                .containsExactly(Assertions.entry("property", "abc"));

        asserter.hasArray("obj/array")
                .isArray()
                .hasNumber("1")
                .hasNull("3")
                .contains(10, 5)
                .containsSequence(10, "str")
                .containsExactly(5, 10, "str", null, "str2")
                .containsOnly(10, 5, "str", "str2", null)
                .doesNotContain(15, "fred")
                .startsWith(5, 10)
                .endsWith(null, "str2")
                .doesNotHaveDuplicates()
                .hasSize(5);

        asserter.hasObject("emptyObject").isEmpty();

        asserter.hasArray("emptyArray").isEmpty();

        asserter.doesNotContain("obj/findme");
    }

    @Test
    public void shouldAssertOnSimpleJsonValue() throws Exception {
        assertThat(json(true)).isBoolean().isTrue();
        assertThat(json("foo")).isString().isEqualTo("foo");
        assertThat(json(1)).isNumber().isInteger().isEqualTo(1);
        assertThat(json(1L)).isNumber().isLong().isEqualTo(1L);
        assertThat(json(3.5)).isNumber().isDouble().isEqualTo(3.5);
        assertThat(json(1)).isInteger().isPositive(); // another assertion for IntegerAssert
        assertThat(json(1L)).isLong().isGreaterThan(0); // another assertion for LongAssert
        assertThat(json(3.5)).isDouble().isBetween(3d, 4d); // another assertion for DoubleAssert
        assertThat(json(set("foo", 42))).isSet().hasSize(2);
    }

    @Test (expectedExceptions = AssertionError.class)
    public void shouldAssertFail() throws Exception {
        assertThat(json(null)).isBoolean();
    }

    @Test (expectedExceptions = AssertionError.class)
    public void testDoesNotContainThrowsException() throws Exception {
        // Given
        JsonValue value = json(object(field("null", null)));

        // When
        AssertJJsonValueAssert.AbstractJsonValueAssert asserter = AssertJJsonValueAssert.assertThat(value);

        // Then
        asserter.doesNotContain("null");
    }

    @Test
    public void testPromisedAssertion() throws Exception {
        // Given
        Promise<JsonValue, Exception> value = newResultPromise(json(object(field("bool", true))));

        // When
        AssertJJsonValueAssert.AssertJJsonValuePromiseAssert asserter = AssertJJsonValueAssert.assertThat(value);

        // Then
        asserter.succeeded().isObject().hasBoolean("bool");
    }

}
