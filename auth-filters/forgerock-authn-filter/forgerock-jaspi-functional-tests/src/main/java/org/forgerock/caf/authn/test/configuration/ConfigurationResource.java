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

package org.forgerock.caf.authn.test.configuration;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.google.inject.Singleton;
import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * <p>CREST resource responsible for exposing the module configuration of the JASPI runtime.</p>
 *
 * <p>The resource offers only two operations, read and update. Performing a read will simply read the JASPI runtime's
 * current module configuration and performing an update will update the runtime's module configuration which will be
 * used for every subsequent request.</p>
 *
 * @since 1.5.0
 */
@Singleton
public class ConfigurationResource implements SingletonResourceProvider {

    private JsonValue moduleConfiguration = json(object(field("serverAuthContext", object())));

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<JsonValue, ResourceException> actionInstance(ServerContext context, ActionRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * Unsupported operation.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> patchInstance(ServerContext context, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException());
    }

    /**
     * Will perform a read of the runtime's module configuration.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> readInstance(ServerContext context, ReadRequest request) {
        return newResultPromise(new Resource("ModuleConfiguration", Integer.toString(moduleConfiguration.hashCode()),
                moduleConfiguration));
    }

    /**
     * Will perform an update of the runtime's module configuration.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> updateInstance(ServerContext context, UpdateRequest request) {
        moduleConfiguration = request.getContent();
        return newResultPromise(new Resource("ModuleConfiguration", Integer.toString(moduleConfiguration.hashCode()),
                moduleConfiguration));
    }
}
