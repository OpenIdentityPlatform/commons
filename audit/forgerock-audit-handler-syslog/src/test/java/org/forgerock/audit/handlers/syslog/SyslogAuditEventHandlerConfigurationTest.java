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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.testng.annotations.Test;

import java.io.IOException;

public class SyslogAuditEventHandlerConfigurationTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void canBeSerializedAsJson() throws Exception {
        // given
        SyslogAuditEventHandlerConfiguration config = new SyslogAuditEventHandlerConfiguration();
        config.setProtocol(TransportProtocol.TCP);
        config.setHost("https://forgerock.example.com");
        config.setPort(6514);
        config.setConnectTimeout(30000);
        config.setFacility(Facility.LOCAL0);
        config.setProductName("OpenAM");

        // when
        String json = objectMapper.writeValueAsString(config);
        SyslogAuditEventHandlerConfiguration config2 =
                objectMapper.readValue(json, SyslogAuditEventHandlerConfiguration.class);

        // then
        assertThat(json).isEqualTo("{" +
                "\"protocol\":\"TCP\"," +
                "\"host\":\"https://forgerock.example.com\"," +
                "\"port\":6514," +
                "\"connectTimeout\":30000," +
                "\"facility\":\"LOCAL0\"," +
                "\"productName\":\"OpenAM\"" +
                "}");
        assertThat(config2).isEqualTo(config);
    }

    @Test
    public void deserializationPermitsConnectionTimeoutToBeSkippedForUdpProtocol() throws IOException {
        // given
        String json = "{" +
                "\"protocol\":\"UDP\"," +
                "\"host\":\"https://forgerock.example.com\"," +
                "\"port\":6514," +
                "\"facility\":\"LOCAL0\"" +
                "}";

        // when
        SyslogAuditEventHandlerConfiguration config = objectMapper.readValue(json, SyslogAuditEventHandlerConfiguration.class);

        // then
        assertThat(config.getProtocol()).isEqualTo(TransportProtocol.UDP);
        assertThat(config.getHost()).isEqualTo("https://forgerock.example.com");
        assertThat(config.getPort()).isEqualTo(6514);
        assertThat(config.getConnectTimeout()).isEqualTo(0);
        assertThat(config.getFacility()).isEqualTo(Facility.LOCAL0);
    }

    @Test(expectedExceptions = InvalidFormatException.class)
    public void deserializationRejectsInvalidProtocol() throws IOException {
        // given
        String json = "{" +
                "\"protocol\":\"INVALID\"," +
                "\"host\":\"https://forgerock.example.com\"," +
                "\"port\":6514," +
                "\"facility\":\"LOCAL0\"," +
                "\"connectTimeout\":30000" +
                "}";

        // when
        objectMapper.readValue(json, SyslogAuditEventHandlerConfiguration.class);
    }

    @Test(expectedExceptions = InvalidFormatException.class)
    public void deserializationRejectsInvalidFacility() throws IOException {
        // given
        String json = "{" +
                "\"protocol\":\"TCP\"," +
                "\"host\":\"https://forgerock.example.com\"," +
                "\"port\":6514," +
                "\"facility\":\"INVALID\"," +
                "\"connectTimeout\":30000" +
                "}";

        // when
        objectMapper.readValue(json, SyslogAuditEventHandlerConfiguration.class);
    }

}
