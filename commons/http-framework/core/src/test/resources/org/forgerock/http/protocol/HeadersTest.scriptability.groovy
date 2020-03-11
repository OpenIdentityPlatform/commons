import org.forgerock.http.header.ConnectionHeader
import org.forgerock.http.protocol.Headers
import org.forgerock.http.protocol.HeadersTest

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

import static org.assertj.core.api.Assertions.*

def HEADER_VALUE = "agiagipa";

def headers1 = new Headers()
def headers2 = new Headers()

headers1.Connection = [ HEADER_VALUE ]
headers2.add("Connection", HEADER_VALUE)

assertThat(headers2.get(ConnectionHeader.class).getValues()).hasSize(1).containsOnly(HEADER_VALUE);

headers2.Connection = [ ]
headers2.add("Connection", [ HEADER_VALUE ])

headers1.put("Connection", [ "123", "456" ])

headers1.add("Connection", "adgipahg")

headers2.Connection = headers1.Connection

assertThat(headers2.get(ConnectionHeader.class).getValues()).hasSize(3);
