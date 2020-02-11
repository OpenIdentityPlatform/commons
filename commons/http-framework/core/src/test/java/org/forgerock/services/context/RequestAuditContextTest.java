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
package org.forgerock.services.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.util.time.TimeService;
import org.testng.annotations.Test;

public class RequestAuditContextTest {

    @Test
    public void shouldCreateContextWithCurrentTime() {
        TimeService time = mock(TimeService.class);
        when(time.now()).thenReturn(1L);

        RequestAuditContext context = new RequestAuditContext(new RootContext(), time);

        assertThat(context.getRequestReceivedTime()).isEqualTo(1L);
    }

}
