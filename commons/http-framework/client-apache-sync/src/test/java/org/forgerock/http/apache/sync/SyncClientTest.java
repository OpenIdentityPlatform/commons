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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.http.apache.sync;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.custom;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBody;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import com.xebialabs.restito.semantics.Predicate;
import com.xebialabs.restito.semantics.Predicates;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHeader;
import org.forgerock.http.Client;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.server.StubServer;

@SuppressWarnings("javadoc")
public class SyncClientTest {

    // TODO: can we test connection pooling, reuse, SSL, HTTP version, etc?

    private StubServer server;

    @BeforeTest
    public void setUp() throws Exception {
        // Create mock HTTP server.
        server = new StubServer().run();
    }

    @AfterTest
    public void tearDown() throws Exception {
        server.stop();
    }

    @BeforeMethod
    public void cleanup() throws Exception {
        // Clear mocked invocations between tests
        // So we can reuse the server instance (less traces) still having isolation
        if (server != null) {
            server.getCalls().clear();
            server.getStubs().clear();
        }
    }

    @Test
    public void shouldSendPostHttpMessageWithEntityContent() throws Exception {
        whenHttp(server).match(post("/test"),
                               withPostBodyContaining("Hello"))
                        .then(status(HttpStatus.OK_200));

        Client client = new Client(new HttpClientHandler());
        Request request = new Request();
        request.setMethod("POST");
        request.setUri(format("http://localhost:%d/test", server.getPort()));
        request.getEntity().setString("Hello");
        assertThat(client.send(request).get().getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void shouldSendPostHttpMessageWithEmptyEntity() throws Exception {
        whenHttp(server).match(post("/test"),
                               not(withPostBody()))
                        .then(status(HttpStatus.OK_200));

        Client client = new Client(new HttpClientHandler());
        Request request = new Request();
        request.setMethod("POST");
        request.setUri(format("http://localhost:%d/test", server.getPort()));
        assertThat(client.send(request).get().getStatus()).isEqualTo(Status.OK);
    }

    @Test
    public void decodesCompressedResponseTransparently() throws Exception {
        stubEncodedResponse("x-gzip");

        try (HttpClientHandler handler = new HttpClientHandler();
             Response response = new Client(handler).send(newGetRequest()).get()) {
            assertThat(response.getStatus()).isEqualTo(Status.OK);
            assertThat(response.getHeaders().get("Content-Encoding")).isNull();
            assertThat(response.getEntity().getString()).isEqualTo("hello");
        }
    }

    @Test
    public void limitsTransparentResponseDecompression() throws Exception {
        String property = "org.openidentityplatform.http.maxDecodedContentBytes";
        String previous = System.setProperty(property, "4");
        try {
            stubEncodedResponse("gzip");
            try (HttpClientHandler handler = new HttpClientHandler();
                 Response response = new Client(handler).send(newGetRequest()).get()) {
                assertThat(response.getStatus()).isEqualTo(Status.OK);
                assertThatThrownBy(() -> response.getEntity().getBytes())
                        .isInstanceOf(IOException.class)
                        .hasMessageContaining("Decoded content exceeds");
            }
        } finally {
            restoreProperty(property, previous);
        }
    }

    @DataProvider
    public Object[][] responseCodings() {
        return new Object[][] {
            { new String[] { "gzip, deflate" } },
            { new String[] { "gzip", "deflate" } }
        };
    }

    @Test(dataProvider = "responseCodings")
    public void decodesStackedResponseCodingsTransparently(String[] codings) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflate = new DeflaterOutputStream(bytes)) {
            deflate.write(gzipHello());
        }
        stubEncodedResponse(bytes.toByteArray(), codings);

        try (HttpClientHandler handler = new HttpClientHandler();
             Response response = new Client(handler).send(newGetRequest()).get()) {
            assertThat(response.getStatus()).isEqualTo(Status.OK);
            assertThat(response.getHeaders().get("Content-Encoding")).isNull();
            assertThat(response.getEntity().getString()).isEqualTo("hello");
        }
    }

    @Test
    public void preservesEncodedResponseWhenOneCodingIsUnsupported() throws Exception {
        byte[] bytes = gzipHello();
        stubEncodedResponse(bytes, "custom-coding, gzip");

        try (HttpClientHandler handler = new HttpClientHandler();
             Response response = new Client(handler).send(newGetRequest()).get()) {
            assertThat(response.getStatus()).isEqualTo(Status.OK);
            assertThat(response.getHeaders().getFirst("Content-Encoding")).isEqualTo("custom-coding, gzip");
            assertThat(response.getEntity().getRawContentInputStream().readAllBytes()).isEqualTo(bytes);
        }
    }

    @Test(dataProvider = "responseCodings")
    public void rejectsTooManyResponseCodingsBeforeDecoding(String[] codings) throws Exception {
        String property = "org.openidentityplatform.http.maxContentEncodingLayers";
        String previous = System.setProperty(property, "1");
        try {
            stubEncodedResponse(codings);
            try (HttpClientHandler handler = new HttpClientHandler();
                 Response response = new Client(handler).send(newGetRequest()).get()) {
                assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
                assertThat(response.getCause()).hasRootCauseMessage("Too many content encodings. Maximum is 1");
            }
        } finally {
            restoreProperty(property, previous);
        }
    }

    @Test
    public void stopsParsingResponseCodingsWhenTheLayerLimitIsExceeded() {
        String property = "org.openidentityplatform.http.maxContentEncodingLayers";
        String previous = System.setProperty(property, "1");
        try {
            Header[] encodings = {
                new BasicHeader(HttpHeaders.CONTENT_ENCODING, "gzip"),
                new BasicHeader(HttpHeaders.CONTENT_ENCODING, "deflate"),
                new BasicHeader(HttpHeaders.CONTENT_ENCODING, "identity") {
                    @Override
                    public HeaderElement[] getElements() {
                        throw new AssertionError("Unexpected parsing after exceeding the content coding limit");
                    }
                }
            };

            assertThatThrownBy(() -> SyncHttpClientProvider.parseContentCodings(encodings))
                    .isInstanceOf(ProtocolException.class)
                    .hasRootCauseMessage("Too many content encodings. Maximum is 1");
        } finally {
            restoreProperty(property, previous);
        }
    }

    private void stubEncodedResponse(String... codings) throws IOException {
        stubEncodedResponse(gzipHello(), codings);
    }

    private static byte[] gzipHello() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write("hello".getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private void stubEncodedResponse(byte[] bytes, String... codings) {
        whenHttp(server).match(get("/test"))
                .then(status(HttpStatus.OK_200), bytesContent(bytes), custom(response -> {
                    for (String coding : codings) {
                        response.addHeader("Content-Encoding", coding);
                    }
                    return response;
                }));
    }

    private Request newGetRequest() {
        return new Request().setMethod("GET").setUri(URI.create(format("http://localhost:%d/test", server.getPort())));
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    /**
     * Restito doesn't provide any way to express a negative condition yet.
     */
    private static Condition not(final Condition condition) {
        return new MyCondition(Predicates.not(condition.getPredicate()));
    }

    /**
     * And Condition has a unique protected constructor.
     */
    private static class MyCondition extends Condition {
        protected MyCondition(final Predicate<Call> predicate) {
            super(predicate);
        }
    }
}
