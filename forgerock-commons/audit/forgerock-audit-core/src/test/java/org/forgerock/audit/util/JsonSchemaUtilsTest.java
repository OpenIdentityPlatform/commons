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

package org.forgerock.audit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.util.Set;

public class JsonSchemaUtilsTest {

    @Test
    public void testGeneratingJsonPointersFromSchema() {
        //given
        JsonValue schema = json(object(
                field("id", "/"),
                field("properties", object(
                        field("timestamp", object(
                                field("id", "timestamp"),
                                field("type", "string")
                        )),
                        field("server", object(
                                field("id", "server"),
                                field("type", "object"),
                                field("properties", object(
                                        field("ip", object(
                                                field("id", "ip"),
                                                field("type", "string")
                                        )),
                                        field("port", object(
                                                field("id", "port"),
                                                field("type", "string")
                                        ))
                                ))
                        )),
                        field("array", object(
                                field("id", "array"),
                                field("type", "array"),
                                field("items", array(
                                        object(
                                                field("id", "0"),
                                                field("type", "string")
                                        ),
                                        object(
                                                field("id", "1"),
                                                field("type", "string")
                                        )
                                ))
                        ))
                ))
        ));

        //when
        Set<String> pointers = JsonSchemaUtils.generateJsonPointers(schema);

        //then
        assertThat(pointers != null);
        assertThat(!pointers.isEmpty());
        assertThat(pointers.contains("/timestamp"));
        assertThat(pointers.contains("/server/ip"));
        assertThat(pointers.contains("/server/port"));
        assertThat(pointers.contains("/array"));
    }
}
