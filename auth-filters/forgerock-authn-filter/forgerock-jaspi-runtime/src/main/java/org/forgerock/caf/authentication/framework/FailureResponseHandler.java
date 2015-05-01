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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.resource.ResourceException;

/**
 * A handler class for rendering failures in the most acceptable supported content type.
 */
class FailureResponseHandler {

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

    private final List<ResourceExceptionHandler> handlers = new ArrayList<ResourceExceptionHandler>();
    private final JsonResourceExceptionHandler defaultHandler;

    FailureResponseHandler() {
        defaultHandler = new JsonResourceExceptionHandler();
        handlers.add(defaultHandler);
    }

    /**
     * Handles the error. The message's request {@code Accept} header preference is parsed, and the most suitable
     * handler is selected. If no specific handler is found, or if the highest quality accepted content type is
     * {@code \*\/\*}, then the {@code JsonResourceExceptionHandler} is used.
     * @param jre
     * @param context
     */
    void handle(ResourceException jre, MessageContext context)  {
        Request request = context.getRequest();
        Response response = context.getResponse();

        response.setStatusAndReason(jre.getCode());

        String acceptHeader = request.getHeaders().getFirst("Accept");
        if (acceptHeader != null) {
            SortedSet<MediaType> acceptedTypes = new TreeSet<MediaType>(ACCEPT_QUALITY_COMPARATOR);
            Matcher m = ACCEPT_HEADER.matcher(acceptHeader);
            int lastGroup = 0;
            while (m.find()) {
                acceptedTypes.add(MediaType.parse(acceptHeader.substring(m.start(), m.start(1))));
                lastGroup = m.end(1);
            }
            acceptedTypes.add(MediaType.parse(acceptHeader.substring(lastGroup)));

            for (MediaType type : acceptedTypes) {
                MediaType forComparison = type.withoutParameters();
                for (ResourceExceptionHandler handler : handlers) {
                    for (MediaType handled : handler.handles()) {
                        if (handled.is(forComparison)) {
                            handler.write(jre, response);
                            return;
                        }
                    }
                }
                if (type.withoutParameters().is(WILDCARD)) {
                    break;
                }
            }
        }
        defaultHandler.write(jre, response);
    }

    /**
     * Register a class to handle {@code ResourceException}s.

     * @param handler The {@code ResourceExceptionHandler}.
     */
    void registerExceptionHandler(ResourceExceptionHandler handler) {
        this.handlers.add(handler);
    }

    /**
     * A default implementation of {@code ResourceExceptionHandler} that renders the exception to JSON.
     */
    static final class JsonResourceExceptionHandler implements ResourceExceptionHandler {

        private static final List<MediaType> MEDIA_TYPES = Arrays.asList(
                MediaType.JSON_UTF_8,
                MediaType.JSON_UTF_8.withoutParameters());

        public List<MediaType> handles() {
            return MEDIA_TYPES;
        }

        public void write(ResourceException jre, Response response)  {
            response.getHeaders().putSingle(ContentTypeHeader.valueOf("application/json; charset=UTF-8"));
            response.setEntity(jre.includeCauseInJsonValue().toJsonValue().asMap());
        }

        @Override
        public String toString() {
            return MEDIA_TYPES.toString();
        }
    }

    @Override
    public String toString() {
        return "Registered Handlers: " + handlers.toString() + ", Default Handler: " + defaultHandler.toString();
    }
}
