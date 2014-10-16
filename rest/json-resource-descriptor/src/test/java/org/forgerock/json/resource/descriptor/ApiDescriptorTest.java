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
package org.forgerock.json.resource.descriptor;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.Requests.newReadRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.descriptor.RelationDescriptor.Multiplicity;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("javadoc")
public final class ApiDescriptorTest {
    private static final class ResolverFactoryImpl implements ResolverFactory {
        private final ApiDescriptor api;

        private ResolverFactoryImpl(final ApiDescriptor api) {
            this.api = api;
        }

        @Override
        public Resolver createResolver(ServerContext context, Request request) {
            return new AbstractResolver() {
                private final List<String> realmList = new LinkedList<String>();

                @Override
                public void getRelationsForResource(final RelationDescriptor relation,
                        final String resourceId,
                        final ResultHandler<Collection<RelationDescriptor>> handler) {
                    System.out.println("Queried " + relation + " : " + resourceId);
                    realmList.add(resourceId);
                    handler.handleResult(relation.getResource().getRelations());
                }

                @Override
                public RequestHandler getRequestHandler(final RelationDescriptor relation)
                        throws ResourceException {
                    if (relation.getResourceUrn().equals(API_URN)) {
                        return Api.newApiDescriptorRequestHandler(api);
                    } else if (relation.getResourceUrn().equals(USERS_URN)) {
                        return new AbstractRequestHandler() {
                            @Override
                            public void handleRead(final ServerContext context,
                                    final ReadRequest request, final ResultHandler<Resource> handler) {
                                System.out.println("Reading user from realm " + realmList);
                                final JsonValue content =
                                        json(object(field("id", request.getResourceName())));
                                handler.handleResult(new Resource(request.getResourceName(), "1",
                                        content));
                            }
                        };
                    } else {
                        throw new NotSupportedException("Relation " + relation + " not supported");
                    }
                }
            };
        }
    }

    private static final Urn API_URN = Urn.valueOf("urn:forgerock:common:resource:api:1.0");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final Urn USERS_URN = Urn.valueOf("urn:forgerock:openam:resource:user:1.0");
    private static final JsonGenerator WRITER;

    static {
        try {
            WRITER = JSON_MAPPER.getFactory().createGenerator(System.out);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        WRITER.configure(Feature.AUTO_CLOSE_TARGET, false);
        WRITER.useDefaultPrettyPrinter();
    }

    @Test
    public void smokeTest() throws Exception {

        // @formatter:off
        final ApiDescriptor api = ApiDescriptor.builder("urn:forgerock:openam:api:repo:1.0")
                                .setDescription("Example OpenAM REST API")
                                .addRelation("", API_URN)
                                    .setMultiplicity(Multiplicity.ONE_TO_ONE)
                                    .build()
                                .addRelation("users", USERS_URN)
                                    .build()
                                .addRelation("groups", "urn:forgerock:openam:resource:group:1.0")
                                    .build()
                                .addRelation("realms", "urn:forgerock:openam:resource:realm:1.0")
                                    .setDescription("An OpenAM realm")
                                    .build()
                                .addResource(API_URN)
                                    .setDescription("Commons Rest API Descriptor")
                                    .setSchema(Schema.builder().build())
                                    .build()
                                .addResource("urn:forgerock:openam:resource:user:1.0")
                                    .setDescription("An OpenAM user")
                                    .addAction("login")
                                        .setDescription("Authenticates a user")
                                        .addParameter("password", "The user's password")
                                        .build()
                                    .setSchema(Schema.builder().build())
                                    .addProfile("urn:forgerock:ldap:profile:schema:1.0",
                                            json(object(field("objectClass", "inetOrgPerson"))))
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
                                    .addRelation("users", USERS_URN)
                                        .addAction("bulk-add")
                                            .setDescription("Bulk add a load of users")
                                            .build()
                                        .build()
                                    .addRelation("groups", "urn:forgerock:openam:resource:group:1.0")
                                        .build()
                                    .addRelation("subrealms", "urn:forgerock:openam:resource:realm:1.0")
                                        .setDescription("An OpenAM sub-realm")
                                        .build()
                                    .build()
                                .build();
        // @formatter:on
        final RequestHandler handler = Api.newApiDispatcher(api, new ResolverFactoryImpl(api));
        final Connection connection = Resources.newInternalConnection(handler);

        System.out.println("#### Reading API Descriptor");
        System.out.println();
        final Resource apiValue = connection.read(new RootContext(), newReadRequest(""));
        WRITER.writeObject(apiValue.getContent().getObject());

        System.out.println();
        System.out.println("#### Reading user realms/com/subrealms/example/users/bjensen");
        System.out.println();
        final Resource bjensen =
                connection.read(new RootContext(),
                        newReadRequest("realms/com/subrealms/example/users/bjensen"));
        WRITER.writeObject(bjensen.getContent().getObject());
    }
}
