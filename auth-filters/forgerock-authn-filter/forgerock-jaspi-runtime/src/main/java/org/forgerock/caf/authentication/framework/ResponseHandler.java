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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static org.forgerock.json.fluent.JsonValue.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.ResourceException;

/**
 * A handler class for rendering {@code AuthenticationException}s in the most acceptable supported
 * content type.
 *
 * @since 2.0.0
 */
class ResponseHandler {

    private static final Pattern ACCEPT_HEADER = Pattern.compile("(?:(?:[^\",]*|(?:.*[^\\\\]\".*[^\\\\]\".*)))( *, *)");
    private static final MediaType WILDCARD = MediaType.parse("*/*");
    private static final String QUALITY_PARAMETER = "q";

    private static final Comparator<MediaType> ACCEPT_QUALITY_COMPARATOR = new Comparator<MediaType>() {
        public int compare(MediaType o1, MediaType o2) {
            int comparison = getQuality(o2).compareTo(getQuality(o1));
            if (comparison == 0) {
                // Wildcards should come after non-wildcards
                comparison = o2.toString().compareTo(o1.toString());
            }
            return comparison;
        }

        private BigDecimal getQuality(MediaType type) {
            List<String> quality = type.parameters().get(QUALITY_PARAMETER);
            return quality.isEmpty() ? BigDecimal.ONE : new BigDecimal(quality.get(0));
        }
    };

    private final List<ResponseWriter> writers = new ArrayList<>();
    private final JsonResponseWriter defaultHandler;

    /**
     * Creates a new {@code ResponseHandler} instance.
     */
    ResponseHandler() {
        defaultHandler = new JsonResponseWriter();
        writers.add(defaultHandler);
    }

    /**
     * <p>Handles the {@code AuthenticationException}.</p>
     *
     * <p>The message's request {@code Accept} header preference is parsed, and the most suitable
     * {@code ResponseWriter} is selected. If no specific writer is found, or if the highest
     * quality accepted content type is {@code \*\/\*}, then the default JSON writer is used.</p>
     *
     * @param context The {@code MessageContext} for the request.
     * @param exception The {@code AuthenticationException} to handle.
     */
    void handle(MessageContext context, AuthenticationException exception)  {
        Request request = context.getRequest();
        String acceptHeader = request.getHeaders().getFirst("Accept");
        getResponseHandler(acceptHeader).write(context, exception);
    }

    private ResponseWriter getResponseHandler(String acceptHeader) {
        if (acceptHeader != null) {
            SortedSet<MediaType> acceptedTypes = new TreeSet<>(ACCEPT_QUALITY_COMPARATOR);
            Matcher m = ACCEPT_HEADER.matcher(acceptHeader);
            int lastGroup = 0;
            while (m.find()) {
                acceptedTypes.add(MediaType.parse(acceptHeader.substring(m.start(), m.start(1))));
                lastGroup = m.end(1);
            }
            acceptedTypes.add(MediaType.parse(acceptHeader.substring(lastGroup)));

            for (MediaType type : acceptedTypes) {
                MediaType forComparison = type.withoutParameters();
                for (ResponseWriter handler : writers) {
                    for (MediaType handled : handler.handles()) {
                        if (handled.is(forComparison)) {
                            return handler;
                        }
                    }
                }
                if (type.withoutParameters().is(WILDCARD)) {
                    break;
                }
            }
        }

        return defaultHandler;
    }

    /**
     * Register a class to handle {@code AuthenticationException}s.
     *
     * @param writer The {@code ResponseWriter}.
     */
    void addResponseWriter(ResponseWriter writer) {
        writers.add(writer);
    }

    @Override
    public String toString() {
        return "Registered Handlers: " + writers.toString() + ", Default Handler: " + defaultHandler.toString();
    }

    /**
     * A default implementation of {@code ResponseWriter} that renders the exception to JSON.
     *
     * @since 2.0.0
     */
    static final class JsonResponseWriter implements ResponseWriter {

        private static final List<MediaType> MEDIA_TYPES = Arrays.asList(
                MediaType.JSON_UTF_8,
                MediaType.JSON_UTF_8.withoutParameters());

        public List<MediaType> handles() {
            return MEDIA_TYPES;
        }

        @Override
        public void write(MessageContext context, AuthenticationException exception)  {
            ResourceException jre;
            if (exception instanceof AuthenticationFailedException) {
                jre = new PermanentException(Status.UNAUTHORIZED.getCode(), exception.getMessage(), null);
            } else if (exception.getCause() instanceof ResourceException) {
                jre = (ResourceException) exception.getCause();
            } else {
                jre = new InternalServerErrorException(exception.getMessage(), exception);
            }
            AuditTrail auditTrail = context.getAuditTrail();
            List<Map<String, Object>> failureReasonList = auditTrail.getFailureReasons();
            if (failureReasonList != null && !failureReasonList.isEmpty()) {
                jre.setDetail(json(object(field("failureReasons", failureReasonList))));
            }
            Response response = context.getResponse();
            response.setStatus(Status.valueOf(jre.getCode()));
            response.getHeaders().putSingle(ContentTypeHeader.valueOf(MediaType.JSON_UTF_8.toString()));
            response.setEntity(jre.includeCauseInJsonValue().toJsonValue().asMap());
        }

        @Override
        public String toString() {
            return MEDIA_TYPES.toString();
        }
    }
}
