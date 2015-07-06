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
package org.forgerock.audit.json;

import java.io.InputStream;

import static org.fest.assertions.api.Assertions.*;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditJsonConfigTest {

    @Test
    public void testConfigureAuditService() throws Exception {
        AuditService auditService = createAndConfigureAuditService();

        AuditServiceConfiguration actualConfig = auditService.getConfig();
        assertThat(actualConfig).isNotNull();
        assertThat(actualConfig.getHandlerForQueries()).isEqualTo("pass-through");
    }

    @Test
    public void testRegisterHandler() throws Exception {
        final AuditService auditService = createAndConfigureAuditService();
        final JsonValue config = AuditJsonConfig.getJson(getResource("/audit-passthrough-handler.json"));

        AuditJsonConfig.registerHandlerToService(config, auditService);

        AuditEventHandler<?> registeredHandler = auditService.getRegisteredHandler("passthrough");
        assertThat(registeredHandler).isNotNull();
    }

    @DataProvider
    public Object[][] eventHandlerBadJsonConfigurations() {
        return new Object[][] {
                { "/audit-passthrough-handler-missing-name.json" },
                { "/audit-passthrough-handler-missing-class.json" },
                { "/audit-passthrough-handler-missing-events.json" }
        };
    }

    @Test(dataProvider="eventHandlerBadJsonConfigurations", expectedExceptions=AuditException.class)
    public void testRegisterHandlerWhenConfigurationIsNotCorrect(String jsonResource) throws Exception {
        final AuditService auditService = createAndConfigureAuditService();
        final JsonValue config = AuditJsonConfig.getJson(getResource(jsonResource));

        AuditJsonConfig.registerHandlerToService(config, auditService);
    }

    private AuditService createAndConfigureAuditService() throws AuditException, ResourceException {
        final AuditService auditService = new AuditService();
        AuditServiceConfiguration serviceConfig =
                AuditJsonConfig.parseAuditServiceConfiguration(getResource("/audit-service.json"));
        auditService.configure(serviceConfig);
        return auditService;
    }

    private InputStream getResource(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }
}
