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
package org.forgerock.audit.filter;

import static java.util.Arrays.asList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

public class ValueOrFieldFilterTest {
    public static final String DEFAULT_VALUE = "value";

    @Test
    public void testExcludingValues() {
        // given
        final List<String> auditTopics = asList("topic1");
        final List<String> excludeValues = asList(
                "/topic1/value",
                "/topic1/other/value");
        final List<String> eventFields = asList(
                "/value",
                "/other/value",
                "/another/value");
        final FilterPolicy filterPolicy = new FilterPolicy();
        filterPolicy.setExcludeIf(excludeValues);
        final JsonValue event = createEvent(eventFields);
        final Filter filter = Filters.newValueOrFieldFilter(auditTopics, filterPolicy);

        // when
        filter.doFilter("topic1", event);

        // then
        Assertions.assertThat(event.size()).isEqualTo(2);
        assertThat(event).doesNotContain("/value");
        assertThat(event).doesNotContain("/other/value");
        assertThat(event).hasString("/another/value");
    }

    private final JsonValue createEvent(final List<String> fields) {
        final JsonValue event = json(object());
        for (final String field: fields) {
            event.putPermissive(new JsonPointer(field), DEFAULT_VALUE);
        }
        return event;
    }
}
