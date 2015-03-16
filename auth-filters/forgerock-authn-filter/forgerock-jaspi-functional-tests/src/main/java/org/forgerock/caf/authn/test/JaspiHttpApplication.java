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

package org.forgerock.caf.authn.test;

import org.forgerock.caf.authn.test.configuration.ConfigurationConnectionFactory;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.io.Buffer;
import org.forgerock.json.resource.http.CrestHandler;
import org.forgerock.util.Factory;

/**
 * HTTP Application for JASPI authentication framework functional tests.
 *
 * @since 2.0.0
 */
public class JaspiHttpApplication implements HttpApplication {

    @Override
    public Handler start() throws HttpApplicationException {
        return CrestHandler.newHandler(ConfigurationConnectionFactory.getConnectionFactory());
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}
