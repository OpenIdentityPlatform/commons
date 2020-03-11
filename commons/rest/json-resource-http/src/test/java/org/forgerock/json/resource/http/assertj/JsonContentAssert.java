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

package org.forgerock.json.resource.http.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.assertj.core.api.AbstractAssert;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;

/**
 * Assertion methods for {@link JsonValue}s that are content of request/responses.
 *
 * <p>To create a new instance of this class, invoke
 * <code>{@link org.forgerock.json.resource.http.Assertions#assertThat(JsonValue) assertThat}(actual)</code>.
 */
@SuppressWarnings("javadoc")
public class JsonContentAssert extends AbstractAssert<JsonContentAssert, JsonValue> {

    public JsonContentAssert(final JsonValue actual) {
        super(actual, JsonContentAssert.class);
    }

    public JsonContentAssert isEqualTo(final JsonValue expected) {
        isNotNull();
        JsonValue filtered = actual.copy();
        filtered.remove(ResourceResponse.FIELD_CONTENT_ID);
        filtered.remove(ResourceResponse.FIELD_CONTENT_REVISION);
        assertThat(Objects.toString(filtered)).isEqualTo(Objects.toString(expected));
        return myself;
    }
}
