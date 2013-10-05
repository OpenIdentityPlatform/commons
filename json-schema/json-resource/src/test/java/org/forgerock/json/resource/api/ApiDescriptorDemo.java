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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.json.resource.api;

import static org.forgerock.json.resource.Requests.newActionRequest;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.api.Api;
import org.forgerock.json.resource.api.ApiDescriptor;
import org.forgerock.json.resource.api.Schema;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public final class ApiDescriptorDemo {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final JsonGenerator WRITER;
    static {
        try {
            WRITER = JSON_MAPPER.getJsonFactory().createJsonGenerator(System.out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        WRITER.configure(Feature.AUTO_CLOSE_TARGET, false);
        WRITER.useDefaultPrettyPrinter();
    }

    @Test
    public void smokeTest() throws Exception {
        // @formatter:off
        ApiDescriptor api = ApiDescriptor.builder("urn:forgerock:openam:api:repo:1.0")
                                .setDescription("Example OpenAM REST API")
                                .addRelation("users", "urn:forgerock:openam:resource:user:1.0")
                                    .build()
                                .addRelation("groups", "urn:forgerock:openam:resource:group:1.0")
                                    .build()
                                .addRelation("realms", "urn:forgerock:openam:resource:realm:1.0")
                                    .setDescription("An OpenAM realm")
                                    .build()
                                .addResource("urn:forgerock:openam:resource:user:1.0")
                                    .setDescription("An OpenAM user")
                                    .addAction("login")
                                        .setDescription("Authenticates a user")
                                        .addParameter("password")
                                        .build()
                                    .setSchema(Schema.builder().build())
                                    .build()
                                .addResource("urn:forgerock:openam:resource:admin:1.0")
                                    .setDescription("An OpenAM administrator")
                                    .setParent("urn:forgerock:openam:resource:user:1.0")
                                    .setSchema(Schema.builder().build())
                                    .build()
                                .addResource("urn:forgerock:openam:resource:group:1.0")
                                    .setDescription("An OpenAM group")
                                    .setSchema(Schema.builder().build())
                                    .build()
                                .addResource("urn:forgerock:openam:resource:realm:1.0")
                                    .setDescription("An OpenAM realm")
                                    .addRelation("users", "urn:forgerock:openam:resource:user:1.0")
                                        .build()
                                    .addRelation("groups", "urn:forgerock:openam:resource:group:1.0")
                                        .build()
                                    .addRelation("subrealms", "urn:forgerock:openam:resource:realm:1.0")
                                        .setDescription("An OpenAM sub-realm")
                                        .build()
                                    .build()
                                .build();
        // @formatter:on

        RequestHandler handler = Api.newApiDescriptorRequestHandler(api);
        Connection connection = Resources.newInternalConnection(handler);

        System.out.println("#### Reading API Descriptor");
        System.out.println();
        JsonValue apiValue = connection.action(new RootContext(), newActionRequest("", "api"));
        WRITER.writeObject(apiValue.getObject());

        //        System.out.println();
        //        System.out.println("#### Reading user com/subrealms/example/users/bjensen");
        //        System.out.println();
        //        Resource bjensen =
        //                connection.read(new RootContext(),
        //                        newReadRequest("com/subrealms/example/users/bjensen"));
        //        WRITER.writeObject(bjensen.getContent().getObject());
    }
}
