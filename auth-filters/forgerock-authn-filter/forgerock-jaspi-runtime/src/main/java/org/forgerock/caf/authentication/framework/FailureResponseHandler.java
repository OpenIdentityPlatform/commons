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

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.json.resource.ResourceException;

/**
 * A handler class for rendering failures in the most acceptable supported content type.
 */
public class FailureResponseHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    public FailureResponseHandler() {
        defaultHandler = new JsonResourceExceptionHandler();
        handlers.add(defaultHandler);
    }

    /**
     * Handles the error. The message's request {@code Accept} header preference is parsed, and the most suitable
     * handler is selected. If no specific handler is found, or if the highest quality accepted content type is
     * {@code \*\/\*}, then the {@code JsonResourceExceptionHandler} is used.
     * @param jre
     * @param messageInfo
     * @throws IOException
     */
    public void handle(ResourceException jre, MessageInfo messageInfo) throws IOException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        response.setStatus(jre.getCode());

        String acceptHeader = request.getHeader("Accept");
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
     * @param handlerClass The implementation class.
     */
    public void registerExceptionHandler(Class<? extends ResourceExceptionHandler> handlerClass) {
        try {
            this.handlers.add(handlerClass.newInstance());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate " + handlerClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access " + handlerClass.getName(), e);
        }
    }

    /**
     * A default implementation of {@code ResourceExceptionHandler} that renders the exception to JSON.
     */
    static class JsonResourceExceptionHandler implements ResourceExceptionHandler {

        private static final List<MediaType> MEDIA_TYPES = Arrays.asList(
                MediaType.JSON_UTF_8,
                MediaType.JSON_UTF_8.withoutParameters());

        public List<MediaType> handles() {
            return MEDIA_TYPES;
        }

        public void write(ResourceException jre, HttpServletResponse response) throws IOException {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), jre.includeCauseInJsonValue().toJsonValue().asMap());
        }
    }
}
