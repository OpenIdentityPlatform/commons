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

package org.forgerock.caf.authn;

import org.forgerock.caf.authn.test.JaspiHttpApplication;
import org.forgerock.caf.authn.test.runtime.GuiceModule;
import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

@GuiceModules(GuiceModule.class)
public class HandlerHolder extends GuiceTestCase {

    protected static Handler handler;

    @BeforeSuite
    public void setupGuiceModules() throws Exception {
        super.setupGuiceModules();
        try {
            handler = new JaspiHttpApplication().start();
        } catch (HttpApplicationException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterSuite
    @Override
    public void teardownGuiceModules() throws Exception {
        super.teardownGuiceModules();
    }
}
