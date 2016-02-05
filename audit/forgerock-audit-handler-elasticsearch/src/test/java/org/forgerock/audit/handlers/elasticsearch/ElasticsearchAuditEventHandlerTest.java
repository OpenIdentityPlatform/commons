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
package org.forgerock.audit.handlers.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.http.Client;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticsearchAuditEventHandlerTest {

    @Test
    public void testQuery() throws Exception{

        // given
        final Promise<Response, NeverThrowsException> promise = mock(Promise.class);
        final Client client = createClient(promise);
        final AuditEventHandler handler = createElasticSearchAuditEventHandler(client);

        // when
        // handler.queryEvents(XXXX)

        // then
        // assert something indicating success
        assertThat(true).isTrue();
    }

    private AuditEventHandler createElasticSearchAuditEventHandler(final Client client) throws Exception {
        final ElasticsearchAuditEventHandlerConfiguration configuration =
                new ElasticsearchAuditEventHandlerConfiguration();
        final Set<String> topics = new HashSet<>();
        topics.add("authentication");
        topics.add("accesss");
        topics.add("activity");
        topics.add("config");
        configuration.setTopics(topics);
        // setup config
        final ElasticsearchAuditEventHandler handler =
                new ElasticsearchAuditEventHandler(
                        configuration,
                        getEventTopicsMetaData(),
                        client);
        return handler;
    }

    private Client createClient(final Promise<Response, NeverThrowsException> promise) {
        final Handler handler = mock(Handler.class);
        final Client client = new Client(handler);
        when(handler.handle(any(Context.class), any(Request.class))).thenReturn(promise);
        return client;
    }

    private EventTopicsMetaData getEventTopicsMetaData() throws Exception {
        Map<String, JsonValue> events = new LinkedHashMap<>();
        try (final InputStream configStream = getClass().getResourceAsStream("/org/forgerock/audit/events.json")) {
            final JsonValue predefinedEventTypes = new JsonValue(new ObjectMapper().readValue(configStream, Map.class));
            for (String eventTypeName : predefinedEventTypes.keys()) {
                events.put(eventTypeName, predefinedEventTypes.get(eventTypeName));
            }
        }
        return new EventTopicsMetaData(events);
    }
}
