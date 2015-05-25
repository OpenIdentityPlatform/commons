package org.forgerock.audit.events;

import static org.forgerock.json.fluent.JsonValue.*;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestVisitor;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
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
 * class MyProductAccessAuditEventBuilder{@code <T extends MyProductAccessAuditEventBuilder<T>>}
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
 *    {@literal @}Override
 *    protected T self() {
 *      return (T) this;
 *    }
 *
 *    ...
 * }
 * </code>
 * </pre>
 */
public class AccessAuditEventBuilder<T extends AccessAuditEventBuilder<T>> extends AuditEventBuilder<T> {

    public static final String MESSAGE_ID = "messageId";
    public static final String AUTHENTICATION_ID = "authenticationId";
    public static final String SERVER = "server";
    public static final String CLIENT = "client";
    public static final String HOST = "host";
    public static final String IP = "ip";
    public static final String PORT = "port";
    public static final String METHOD = "method";
    public static final String ACTION = "action";
    public static final String RESOURCE_OPERATION = "resourceOperation";
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

    private static final String HTTP_CONTEXT_NAME = "http";
    private static final String HTTP_CONTEXT_REMOTE_ADDRESS = "remoteAddress";

    private static final Logger logger = LoggerFactory.getLogger(AuditEventBuilder.class);
    private static final ResourceOperationRequestVisitor RESOURCE_OPERATION_VISITOR = new ResourceOperationRequestVisitor();

    /**
     * Starts to build an audit access event.
     * <p>
     * Note: it is preferable to use a specialized builder that allow to add
     * fields specific to a product.
     *
     * @return an audit access event builder
     */
    public static AccessAuditEventBuilder<?> accessEvent() {
        return new AccessAuditEventBuilder();
    }

    /**
     * Sets the provided message id for the event.
     *
     * @param id the message id.
     * @return this builder
     */
    public final T messageId(String id) {
        jsonValue.put(MESSAGE_ID, id);
        return self();
    }

    /**
     * Sets the provided authentication id for the event.
     *
     * @param id the authentication id.
     * @return this builder
     */
    public final T authenticationId(String id) {
        jsonValue.put(AUTHENTICATION_ID, id);
        return self();
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
     * Sets the provided resource operation method and action for the event.
     *
     * @param method the method of the operation (for CREST, expect one method in CRUDPAQ).
     * @param action the detailed action of the operation.
     * @return this builder
     */
    public final T resourceOperation(String method, String action) {
        JsonValue object = json(object(
                field(METHOD, method),
                field(ACTION, action)));
        jsonValue.put(RESOURCE_OPERATION, object);
        return self();
    }

    /**
     * Sets the provided resource operation method for the event.
     *
     * If the method is an Action, use {@code resourceOperation(String, String)} method instead.
     *
     * @param method the method of the operation (for CREST, expect one method in CRUDPQ).
     * @return this builder
     */
    public final T resourceOperation(String method) {
        JsonValue object = json(object(field(METHOD, method)));
        jsonValue.put(RESOURCE_OPERATION, object);
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
     * Sets transactionId from ID of {@link RootContext}, iff the provided
     * <code>Context</code> contains a <code>RootContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T transactionIdFromRootContext(Context context) {
        if (context.containsContext(RootContext.class)) {
            RootContext rootContext = context.asContext(RootContext.class);
            transactionId(rootContext.getId());
        }
        return self();
    }

    /**
     * Sets authenticationId from {@link SecurityContext}, iff the provided
     * <code>Context</code> contains a <code>SecurityContext</code>.
     *
     * @param context The CREST context.
     * @return this builder
     */
    public final T authenticationIdFromSecurityContext(Context context) {
        if (context.containsContext(SecurityContext.class)) {
            SecurityContext securityContext = context.asContext(SecurityContext.class);
            authenticationId(securityContext.getAuthenticationId());
        }
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
        JsonValue object = request.accept(RESOURCE_OPERATION_VISITOR, null);
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
     * @see #authenticationIdFromSecurityContext(Context)
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
     * @see #authenticationIdFromSecurityContext(Context)
     * @see #resourceOperationFromRequest(Request)
     *
     * @return this builder
     */
    protected T forHttpCrestRequest(Context context, Request request, DnsUtils dnsUtils) {
        transactionIdFromRootContext(context);
        clientFromHttpContext(context, dnsUtils);
        httpFromHttpContext(context);
        authenticationIdFromSecurityContext(context);
        resourceOperationFromRequest(request);
        return self();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
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
     * Builds "responseOperation" value for CREST Request.
     */
    private static class ResourceOperationRequestVisitor implements RequestVisitor<JsonValue, Void> {
        @Override
        public JsonValue visitActionRequest(Void ignored, ActionRequest request) {
            return json(object(
                    field(METHOD, request.getRequestType().toString()),
                    field(ACTION, request.getAction())));
        }

        @Override
        public JsonValue visitCreateRequest(Void ignored, CreateRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
        }

        @Override
        public JsonValue visitDeleteRequest(Void ignored, DeleteRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
        }

        @Override
        public JsonValue visitPatchRequest(Void ignored, PatchRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
        }

        @Override
        public JsonValue visitQueryRequest(Void ignored, QueryRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
        }

        @Override
        public JsonValue visitReadRequest(Void ignored, ReadRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
        }

        @Override
        public JsonValue visitUpdateRequest(Void ignored, UpdateRequest request) {
            return json(object(field(METHOD, request.getRequestType().toString())));
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