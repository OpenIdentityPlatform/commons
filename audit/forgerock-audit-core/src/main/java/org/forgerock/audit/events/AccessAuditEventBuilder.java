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
package org.forgerock.audit.events;

import static org.forgerock.audit.events.AuditEventBuilderUtil.*;
import static org.forgerock.json.fluent.JsonValue.*;


import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builder for audit access events.
 * <p>
 * This builder should not be used directly but be specialized for each product to allow to define
 * new specific fields, e.g
 * <pre>
 * <code>
 * class OpenProductAccessAuditEventBuilder{@code <T extends OpenProductAccessAuditEventBuilder<T>>}
 extends AccessAuditEventBuilder{@code <T>} {
 *
 *    protected OpenProductAccessAuditEventBuilder(DnsUtils dnsUtils) {
 *        super(dnsUtils);
 *    }
 *
 *    public static {@code <T>} OpenProductAccessAuditEventBuilder{@code <?>} productAccessEvent() {
 *       return new OpenProductAccessAuditEventBuilder(new DnsUtils());
 *    }
 *
 *    public T someField(String v) {
 *      jsonValue.put("someField", v);
 *      return self();
 *    }
 *
 *    ...
 * }
 * </code>
 * </pre>
 *
 * @param <T> the type of the builder
 */
public class AccessAuditEventBuilder<T extends AccessAuditEventBuilder<T>> extends AuditEventBuilder<T> {

    public static final String SERVER = "server";
    public static final String CLIENT = "client";
    public static final String HOST = "host";
    public static final String IP = "ip";
    public static final String PORT = "port";
    public static final String RESOURCE_OPERATION = AuditEventBuilderUtil.RESOURCE_OPERATION;
    public static final String URI = AuditEventBuilderUtil.URI;
    public static final String PROTOCOL = AuditEventBuilderUtil.PROTOCOL;
    public static final String OPERATION = AuditEventBuilderUtil.OPERATION;
    public static final String METHOD = AuditEventBuilderUtil.METHOD;
    public static final String DETAIL = AuditEventBuilderUtil.DETAIL;
    public static final String COMPONENT = "component";
    public static final String ID = "id";
    public static final String ROLES = "roles";
    public static final String AUTHORIZATION_ID = "authorizationId";
    public static final String PATH = "path";
    public static final String QUERY_STRING = "queryString";
    public static final String HEADERS = "headers";
    public static final String HTTP = "http";
    public static final String STATUS = "status";
    public static final String ELAPSED_TIME = "elapsedTime";
    public static final String RESPONSE = "response";
    public static final String MESSAGE = "message";

    private static final String HOST_HEADER = "Host";
    private static final String HTTP_CONTEXT_NAME = "http";
    private static final String SECURITY_CONTEXT_NAME = "security";
    private static final String HTTP_CONTEXT_REMOTE_ADDRESS = "remoteAddress";

    private static final Logger logger = LoggerFactory.getLogger(AccessAuditEventBuilder.class);

    /**
     * Starts to build an audit access event.
     * <p>
     * Note: it is preferable to use a specialized builder that allow to add
     * fields specific to a product.
     *
     * @return an audit access event builder
     */
    @SuppressWarnings("rawtypes")
    public static AccessAuditEventBuilder<?> accessEvent() {
        return new AccessAuditEventBuilder();
    }

    /**
     * Sets the provided server values for the event.
     *
     * @param ip the ip of the server.
     * @param port the port of the server.
     * @return this builder
     */
    public final T server(String ip, int port) {
        return server(ip, port, null);
    }

    /**
     * Sets the provided server hostname, ip and port for the event.
     * @param ip the ip of the server.
     * @param port the port of the server.
     * @param host the hostname of the server.
     *
     * @return this builder
     */
    public final T server(String ip, int port, String host) {
        JsonValue object = json(object(
                field(HOST, host),
                field(IP, ip),
                field(PORT, port)));
        jsonValue.put(SERVER, object);
        return self();
    }

    /**
     * Sets the server fields for the event, iff the provided
     * <code>Context</code> contains a <code>HttpContext</code>..
     *
     * @param context the CREST context
     * @return this builder
     */
    public final T serverFromHttpContext(Context context) {
        if (context.containsContext(HTTP_CONTEXT_NAME)) {
            JsonValue httpContext = context.getContext(HTTP_CONTEXT_NAME).toJsonValue();
            final String hostHeader = httpContext.get(HEADERS).get(HOST_HEADER).get(0).asString();
            final String[] hostHeaderParts = hostHeader.split(":");
            if (hostHeaderParts.length == 2) {
                server(null, Integer.parseInt(hostHeaderParts[1]), hostHeaderParts[0]);
            }
        }
        return self();
    }

    /**
     * Sets the provided client ip and port for the event.
     *
     * @param ip the ip of the client.
     * @param port the port of the client.
     * @return this builder
     */
    public final T client(String ip, int port) {
        return client(ip, port, null);
    }

    /**
     * Sets the provided client hostname, ip and port for the event.
     *
     * @param ip the ip of the client.
     * @param port the port of the client.
     * @param host the hostname of the client.
     *
     * @return this builder
     */
    public final T client(String ip, int port, String host) {
        JsonValue object = json(object(
                field(HOST, host),
                field(IP, ip),
                field(PORT, port)));
        jsonValue.put(CLIENT, object);
        return self();
    }

    /**
     * Sets the provided client ip for the event.
     *
     * @param ip the ip of the client.
     * @return this builder
     */
    public final T client(String ip) {
        return client(ip, null);
    }

    /**
     * Sets the provided client hostname and ip for the event.
     *
     * @param ip the ip of the client.
     * @param host the hostname of the client.
     *
     * @return this builder
     */
    public final T client(String ip, String host) {
        JsonValue object = json(object(
                field(HOST, host),
                field(IP, ip)));
        jsonValue.put(CLIENT, object);
        return self();
    }

    /**
     * Sets the provided resourceOperation details for the event.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of
     *  CRUDPAQ).
     * @param operationDetail further defines the operation type (e.g. specifies the name of the CRUDPAQ action).
     * @return this builder
     */
    public final T resourceOperation(String uri, String protocol, String operationMethod, String operationDetail) {
        jsonValue.put(RESOURCE_OPERATION, createResourceOperation(uri, protocol, operationMethod, operationDetail));
        return self();
    }

    /**
     * Sets the provided resourceOperation details for the event.
     *
     * @param uri the resource identifier.
     * @param protocol the scheme of the resource identifier uri.
     * @param operationMethod the type of operation (e.g. when protocol is CREST, operation type will be one of
     *  CRUDPAQ).
     * @return this builder
     */
    public final T resourceOperation(String uri, String protocol, String operationMethod) {
        jsonValue.put(RESOURCE_OPERATION, createResourceOperation(uri, protocol, operationMethod));
        return self();
    }

    /**
     * Sets the provided authorizationId fields for the event.
     *
     * @param component the component part of the authorization id.
     * @param id the id part of the authorization id.
     * @param roles the list of roles. Roles are optional.
     * @return this builder
     */
    public final T authorizationId(String component, String id, String...roles) {
        JsonValue object = json(object(
                field(COMPONENT, component),
                field(ID, id)));
        if (roles != null && roles.length > 0) {
            Object roleList = json(array(Arrays.copyOf(roles, roles.length, Object[].class)));
            object.put(ROLES, roleList);
        }
        jsonValue.put(AUTHORIZATION_ID, object);
        return self();
    }

    /**
     * Sets the provided authorizationId fields for the event, iff the provided
     * <code>Context</code> contains a <code>SecurityContext</code>..
     *
     * @param context the CREST context
     * @return this builder
     */
    public final T authorizationIdFromSecurityContext(Context context) {
        if (context.containsContext(SECURITY_CONTEXT_NAME)) {
            JsonValue securityContext = context.getContext(SECURITY_CONTEXT_NAME).toJsonValue();
            authorizationId(
                    securityContext.get(AUTHORIZATION_ID).get(COMPONENT).asString(),
                    securityContext.get(AUTHORIZATION_ID).get(ID).asString(),
                    getRoles(securityContext));
        }
        return self();
    }

    private String[] getRoles(JsonValue securityContext) {
        if (securityContext.get(AUTHORIZATION_ID).isDefined(ROLES)) {
            return securityContext.get(AUTHORIZATION_ID).get(ROLES).asList().toArray(new String[0]);
        }
        return null;
    }

    /**
     * Sets the provided HTTP fields for the event.
     *
     * @param method the HTTP method.
     * @param path the path of HTTP request.
     * @param queryString the query string of HTTP request.
     * @param headers the list of headers of HTTP request. The headers are optional.
     * @return this builder
     */
    public final T http(String method, String path, String queryString, Map<String, List<String>> headers) {
        JsonValue object = json(object(
                field(METHOD, method),
                field(PATH, path),
                field(QUERY_STRING, queryString),
                field(HEADERS, headers)));
        jsonValue.put(HTTP, object);
        return self();
    }

    /**
     * Sets the provided response for the event.
     *
     * @param status the status of the operation.
     * @param elapsedTime the execution time of the action.
     * @return this builder
     */
    public final T response(String status, long elapsedTime) {
        JsonValue object = json(object(
                field(STATUS, status),
                field(ELAPSED_TIME, elapsedTime)));
        jsonValue.put(RESPONSE, object);
        return self();
    }

    /**
     * Sets the provided response for the event, with an additional message.
     *
     * @param status the status of the operation.
     * @param elapsedTime the execution time of the action.
     * @param message the message associated to the status.
     * @return this builder
     */
    public final T responseWithMessage(String status, long elapsedTime, String message) {
        JsonValue object = json(object(
                field(STATUS, status),
                field(ELAPSED_TIME, elapsedTime),
                field(MESSAGE, message)));
        jsonValue.put(RESPONSE, object);
        return self();
    }

    /**
     * Sets client ip, port and host from <code>HttpContext</code>, iff the provided
     * <code>Context</code> contains a <code>HttpContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T clientFromHttpContext(Context context) {
        return clientFromHttpContext(context, new DnsUtils());
    }

    /**
     * Sets client ip, port and host from <code>HttpContext</code>, iff the provided
     * <code>Context</code> contains a <code>HttpContext</code>.
     *
     * This method is visible for testing so that DNS lookup can be mocked.
     *
     * @param context The CREST context.
     * @param dnsUtils Delegate responsible for DNS lookup.
     * @return this builder
     */
    protected final T clientFromHttpContext(Context context, DnsUtils dnsUtils) {
        if (context.containsContext(HTTP_CONTEXT_NAME)) {
            JsonValue httpContext = context.getContext(HTTP_CONTEXT_NAME).toJsonValue();
            String ipAddress = httpContext.get(HTTP_CONTEXT_REMOTE_ADDRESS).asString();
            String hostName = null;
            try {
                hostName = dnsUtils.getHostName(ipAddress);
            } catch (UnknownHostException e) {
                logger.debug("Unable to lookup client host name for {}.", ipAddress);
            }
            client(ipAddress, hostName);
        }
        return self();
    }

    /**
     * Sets HTTP method, path, queryString and headers from <code>HttpContext</code>, iff the provided
     * <code>Context</code> contains a <code>HttpContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T httpFromHttpContext(Context context) {
        if (context.containsContext(HTTP_CONTEXT_NAME)) {
            JsonValue httpContext = context.getContext(HTTP_CONTEXT_NAME).toJsonValue();

            http(httpContext.get("method").asString(),
                    httpContext.get("path").asString(),
                    buildQueryString(httpContext.get("parameters").asMapOfList(String.class)),
                    httpContext.get("headers").asMapOfList(String.class));
        }
        return self();
    }

    /**
     * Sets resourceOperation method from {@link Request}; iff the provided <code>Request</code>
     * is an {@link ActionRequest} then resourceOperation action will also be set.
     *
     * @param request The CREST request.
     * @return this builder
     */
    public final T resourceOperationFromRequest(Request request) {
        JsonValue object = createResourceOperationFromRequest(request);
        jsonValue.put(RESOURCE_OPERATION, object);
        return self();
    }

    /**
     * Sets common fields from CREST contexts and request.
     *
     * @param context The CREST context.
     * @param request The CREST request.
     *
     * @see #transactionIdFromRootContext(Context)
     * @see #clientFromHttpContext(Context)
     * @see #httpFromHttpContext(Context)
     * @see #authenticationFromSecurityContext(Context)
     * @see #resourceOperationFromRequest(Request)
     *
     * @return this builder
     */
    public final T forHttpCrestRequest(Context context, Request request) {
        return forHttpCrestRequest(context, request, new DnsUtils());
    }

    /**
     * Sets common fields from CREST contexts and request.
     *
     * This method is visible for testing so that DNS lookup can be mocked.
     *
     * @param context The CREST context.
     * @param request The CREST request.
     * @param dnsUtils Delegate responsible for DNS lookup.
     *
     * @see #transactionIdFromRootContext(Context)
     * @see #clientFromHttpContext(Context)
     * @see #httpFromHttpContext(Context)
     * @see #authenticationFromSecurityContext(Context)
     * @see #resourceOperationFromRequest(Request)
     *
     * @return this builder
     */
    protected T forHttpCrestRequest(Context context, Request request, DnsUtils dnsUtils) {
        transactionIdFromRootContext(context);
        clientFromHttpContext(context, dnsUtils);
        httpFromHttpContext(context);
        authenticationFromSecurityContext(context);
        resourceOperationFromRequest(request);
        return self();
    }

    private String buildQueryString(Map<String, List<String>> parameters) {
        StringBuilder sb = new StringBuilder();
        boolean valueSeen = false;
        if (parameters != null) {
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                for (String value : entry.getValue()) {
                    if (valueSeen) {
                        sb.append("&");
                    }
                    sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(value));
                    valueSeen = true;
                }
            }
        }
        return sb.toString();
    }

    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    /**
     * Encapsulates DNS utilities into an object that can be mocked from unit tests.
     */
    protected static class DnsUtils {

        /**
         * Gets the host name for this IP address.
         *
         * @param ipAddress An IP address (supports v4 and v6 addresses).
         * @return The host for the specified IP address or null if reverse DNS lookup could not be completed.
         */
        public String getHostName(String ipAddress) throws UnknownHostException {
            InetAddress ipAddr = InetAddress.getByName(ipAddress);
            return ipAddr.getHostName();
        }
    }

}
