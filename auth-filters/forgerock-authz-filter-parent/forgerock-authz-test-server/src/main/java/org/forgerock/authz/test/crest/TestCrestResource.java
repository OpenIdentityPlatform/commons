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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.authz.test.crest;

import java.util.Map;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * A fake CREST Resource which will only respond to Read requests.
 *
 * It will return the passed in contents of the {@link SecurityContext} as the JSON
 * body.
 */
public final class TestCrestResource implements CollectionResourceProvider {

    /**
     * Generates an exception and instructs the passed-in handler to handle it immediately.
     *
     * @param handler The Handler which will handle the generated {@link NotSupportedException}
     */
    private static void generateUnsupportedOperation(ResultHandler handler) {
        NotSupportedException exception = new NotSupportedException("Operation is not supported.");
        handler.handleError(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext serverContext, ActionRequest actionRequest,
                                 ResultHandler<JsonValue> jsonValueResultHandler) {
        generateUnsupportedOperation(jsonValueResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext serverContext, String s, ActionRequest actionRequest,
                               ResultHandler<JsonValue> jsonValueResultHandler) {
        generateUnsupportedOperation(jsonValueResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext serverContext, CreateRequest createRequest,
                               ResultHandler<Resource> resourceResultHandler) {
        generateUnsupportedOperation(resourceResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext serverContext, String s, DeleteRequest deleteRequest,
                               ResultHandler<Resource> resourceResultHandler) {
        generateUnsupportedOperation(resourceResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext serverContext, String s, PatchRequest patchRequest,
                              ResultHandler<Resource> resourceResultHandler) {
        generateUnsupportedOperation(resourceResultHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext serverContext, QueryRequest queryRequest,
                                QueryResultHandler queryResultHandler) {
        generateUnsupportedOperation(queryResultHandler);
    }

    /**
     * Reads the values placed in the request's {@link org.forgerock.authz.AuthorizationContext}
     * (via its {@link SecurityContext}) and returns those as the contents of the response in
     * a new JSON object.
     *
     * @param serverContext {@inheritDoc}
     * @param s {@inheritDoc}
     * @param readRequest {@inheritDoc}
     * @param resourceResultHandler {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext serverContext, String s, ReadRequest readRequest,
                             ResultHandler<Resource> resourceResultHandler) {

        //fetch data from security context
        final SecurityContext securityContext = serverContext.asContext(SecurityContext.class);
        final Map<String, Object> authId = securityContext.getAuthorizationId();

        resourceResultHandler.handleResult(new Resource("testResource", "0", new JsonValue(authId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext serverContext, String s, UpdateRequest updateRequest,
                               ResultHandler<Resource> resourceResultHandler) {
        generateUnsupportedOperation(resourceResultHandler);
    }
}
