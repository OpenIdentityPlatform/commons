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
package org.forgerock.audit.handlers.syslog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.AuditServiceBuilder.newAuditService;

import org.forgerock.audit.AuditService;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.io.InputStream;

@SuppressWarnings("javadoc")
public class SyslogHandlerTest {

    /**
     * Integration test.
     */
    @Test
    public void canConfigureSyslogHandlerFromJsonAndRegisterWithAuditService() throws Exception {
        // given
        final AuditServiceBuilder auditServiceBuilder = newAuditService();
        final JsonValue config = AuditJsonConfig.getJson(getResource("/event-handler-config.json"));

        // when
        AuditJsonConfig.registerHandlerToService(config, auditServiceBuilder);

        // then
        AuditService auditService = auditServiceBuilder.build();
        auditService.startup();
        AuditEventHandler registeredHandler = auditService.getRegisteredHandler("syslog");
        assertThat(registeredHandler).isNotNull();
    }

    private InputStream getResource(String resourceName) {
        return getClass().getResourceAsStream(resourceName);
    }
}
