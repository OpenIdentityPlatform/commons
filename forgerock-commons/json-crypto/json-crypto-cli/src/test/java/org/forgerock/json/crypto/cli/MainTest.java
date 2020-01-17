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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.crypto.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.testng.annotations.Test;

import java.net.URLDecoder;
import java.util.Map;

public class MainTest {

    @Test
    public void testMain() throws Exception {

        String[] args = new String[13];
        args[0] = "-encrypt";
        args[1] = "-keystore";
        args[2] = URLDecoder.decode(MainTest.class.getResource("/keystore.jceks").getPath(), "UTF-8");
        args[3] = "-storetype";
        args[4] = "JCEKS";
        args[5] = "-storepass";
        args[6] = "changeit";
        args[7] = "-alias";
        args[8] = "openidm-sym-default";
        args[9] = "-srcjson";
        args[10] = URLDecoder.decode(MainTest.class.getResource("/clean.json").getPath(), "UTF-8");

        args[11] = "-destjson";
        args[12] = URLDecoder.decode(MainTest.class.getResource("/").toURI().resolve("encrypted.json").getPath(),
                "UTF-8");

        // encrypt the file
        Main.main(args);

        args[0] = "-decrypt";
        args[10] = URLDecoder.decode(MainTest.class.getResource("/encrypted.json").getPath(), "UTF-8");
        args[12] = URLDecoder.decode(MainTest.class.getResource("/").toURI().resolve("decrypted.json").getPath(),
                "UTF-8");

        // decrypt the file
        Main.main(args);

        ObjectMapper mapper = new ObjectMapper();
        JsonValue expected = new JsonValue(mapper.readValue(MainTest.class.getResourceAsStream("/clean.json"),
                Map.class));
        JsonValue actual = new JsonValue(mapper.readValue(MainTest.class.getResourceAsStream("/decrypted.json"),
                Map.class));
        assertThat(actual.getObject()).isEqualTo(expected.getObject());
    }
}
