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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;
import static org.forgerock.audit.json.AuditJsonConfig.parseAuditServiceConfiguration;

import java.io.InputStream;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.json.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AuditJsonConfigTest {

    @Test
    public void testConfigureAuditService() throws Exception {
        AuditService auditService = newAuditService().withConfiguration(loadConfiguration()).build();
        auditService.startup();

        AuditServiceConfiguration actualConfig = auditService.getConfig();
        assertThat(actualConfig).isNotNull();
        assertThat(actualConfig.getHandlerForQueries()).isEqualTo("pass-through");
        assertThat(actualConfig.getAvailableAuditEventHandlers())
                .containsOnly("org.forgerock.audit.PassThroughAuditEventHandler");
    }

    @Test
    public void testRegisterHandler() throws Exception {
        AuditServiceBuilder auditServiceBuilder = newAuditService().withConfiguration(loadConfiguration());
        final JsonValue config = loadJsonValue("/audit-passthrough-handler.json");

        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);

        AuditService auditService = auditServiceBuilder.build();
        auditService.startup();
        AuditEventHandler registeredHandler = auditService.getRegisteredHandler("passthrough");
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
        AuditServiceBuilder auditServiceBuilder = newAuditService().withConfiguration(loadConfiguration());
        final JsonValue config = loadJsonValue(jsonResource);

        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);
    }

    @Test
    public void testGetAuditEventHandlerConfigurationSchema() throws Exception {
        //given
        final JsonValue expectedSchema = loadJsonValue("/audit-passthrough-handler-schema.json");

        //when
        final JsonValue schema =
                AuditJsonConfig.getAuditEventHandlerConfigurationSchema(
                        "org.forgerock.audit.PassThroughAuditEventHandler", getClass().getClassLoader());

        //then
        assertThat(schema.asMap()).isEqualTo(expectedSchema.asMap());
    }

    private AuditServiceConfiguration loadConfiguration() throws AuditException {
        return parseAuditServiceConfiguration(getResource("/audit-service.json"));
    }

    private JsonValue loadJsonValue(String path) throws AuditException {
        return AuditJsonConfig.getJson(getResource(path));
    }

    private InputStream getResource(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }
}
