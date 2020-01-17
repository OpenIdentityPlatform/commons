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

package org.forgerock.json.resource;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;

/**
 * Version of {@link SynchronousRequestHandlerAdapter} that exposes a described handler.
 */
public class DescribedSyncRequestHandlerAdapter extends SynchronousRequestHandlerAdapter
        implements Describable<ApiDescription, Request> {

    private final Describable<ApiDescription, Request> described;

    @SuppressWarnings("unchecked")
    DescribedSyncRequestHandlerAdapter(SynchronousRequestHandler syncHandler) {
        super(syncHandler);
        if (!(syncHandler instanceof Describable)) {
            throw new IllegalArgumentException("Handler must be Describable");
        }
        this.described = (Describable<ApiDescription, Request>) syncHandler;
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> producer) {
        return described.api(producer);
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return described.handleApiRequest(context, request);
    }

    @Override
    public void addDescriptorListener(Describable.Listener listener) {
        described.addDescriptorListener(listener);
    }

    @Override
    public void removeDescriptorListener(Describable.Listener listener) {
        described.removeDescriptorListener(listener);
    }
}
