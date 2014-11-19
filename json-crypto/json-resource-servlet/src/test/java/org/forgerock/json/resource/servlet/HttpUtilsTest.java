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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.json.resource.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.http.test.HttpTest.newRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.Response;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class HttpUtilsTest {
    private static final String JPEG_PART_NAME = "profile";
    private static final String JPEG_PART_FILENAME = "file.jpg";
    private static final String JPEG_CONTENT = "This is Jpeg Content";

    private static final String PROFILE_FILENAME_KEY = "profileFilename";
    private static final String PROFILE_CONTENT_KEY = "profileContent";
    private static final String PROFILE_MIMETYPE_KEY = "profileMimeType";
    private static final String UID = "uid";
    private static final String ALICE = "alice";

    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private static final String REQUEST_CONTENT_TYPE =
            "multipart/form-data; boundary=kwkIqb-fsdbtcNpB4dJ_Xqf1-3b0Hp_VF9D0vsgL";

    private org.forgerock.http.Request request;

    private byte[] requestInputStreamData;

    private String jsonBody;
    private String jsonPatchBody;

    @BeforeClass
    public void onTimeSetup() {
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{").append("\n");
        jsonBody.append("\"uid\" : \"alice\",").append("\n");
        jsonBody.append("\"email\" : \"alice@example.com\",").append("\n");
        jsonBody.append(
                "\"" + PROFILE_CONTENT_KEY + "\" : { \"$ref\" : \"cid:" + JPEG_PART_NAME
                        + "#content\" },").append("\n");
        jsonBody.append(
                "\"" + PROFILE_MIMETYPE_KEY + "\" : { \"$ref\" : \"cid:" + JPEG_PART_NAME
                        + "#mimetype\" },").append("\n");
        jsonBody.append(
                "\"" + PROFILE_FILENAME_KEY + "\" : { \"$ref\" : \"cid:" + JPEG_PART_NAME
                        + "#filename\" }").append("\n");
        jsonBody.append("}");
        this.jsonBody = jsonBody.toString();

        jsonBody = new StringBuilder();
        jsonBody.append("[");
        jsonBody.append("{ \"operation\": \"replace\", \"field\": \"" + PROFILE_MIMETYPE_KEY
                + "\", \"value\": { \"$ref\" : \"cid:" + JPEG_PART_NAME + "#mimetype\" } },");
        jsonBody.append("{ \"operation\": \"replace\", \"field\": \"" + PROFILE_CONTENT_KEY
                + "\", \"value\": { \"$ref\" : \"cid:" + JPEG_PART_NAME + "#content\" } },");
        jsonBody.append("{ \"operation\": \"replace\", \"field\": \"" + PROFILE_FILENAME_KEY
                + "\", \"value\": { \"$ref\" : \"cid:" + JPEG_PART_NAME + "#filename\" } }");
        jsonBody.append("]");
        this.jsonPatchBody = jsonBody.toString();
    }

    private void createMultiPartRequest(String jsonBody) {
        ByteArrayOutputStream requestInputStreamData = new ByteArrayOutputStream();

        PrintStream reguestInputStreamData = new PrintStream(requestInputStreamData);
        reguestInputStreamData.println("Content-Type: " + REQUEST_CONTENT_TYPE);
        reguestInputStreamData.println();
        reguestInputStreamData.println("--kwkIqb-fsdbtcNpB4dJ_Xqf1-3b0Hp_VF9D0vsgL");
        reguestInputStreamData.println("Content-Disposition: form-data; name=\"json\"");
        reguestInputStreamData.println("Content-Type: application/json; charset=US-ASCII");
        reguestInputStreamData.println("Content-Transfer-Encoding: 8bit");
        reguestInputStreamData.println();
        reguestInputStreamData.println(jsonBody);
        reguestInputStreamData.println("--kwkIqb-fsdbtcNpB4dJ_Xqf1-3b0Hp_VF9D0vsgL");
        reguestInputStreamData.println("Content-Disposition: form-data; name=\"" + JPEG_PART_NAME
                + "\"; filename=\"" + JPEG_PART_FILENAME + "\"");
        reguestInputStreamData.println("Content-Type: " + JPEG_CONTENT_TYPE);
        reguestInputStreamData.println();
        reguestInputStreamData.println(JPEG_CONTENT);
        reguestInputStreamData.println("--kwkIqb-fsdbtcNpB4dJ_Xqf1-3b0Hp_VF9D0vsgL");

        reguestInputStreamData.close();
        this.requestInputStreamData = requestInputStreamData.toByteArray();
    }

    private void createRequest(String jsonBody) {
        ByteArrayOutputStream requestInputStreamData = new ByteArrayOutputStream();

        PrintStream reguestInputStreamData = new PrintStream(requestInputStreamData);
        reguestInputStreamData.println(jsonBody);

        reguestInputStreamData.close();
        this.requestInputStreamData = requestInputStreamData.toByteArray();
    }

    private void setUpRequestMock(org.forgerock.http.Request request, String contentType)
            throws IOException {
        request.getHeaders().putSingle(ContentTypeHeader.NAME, contentType);
        request.setEntity(requestInputStreamData);
    }

    private void testMultiPartResult(JsonValue result) {
        assertThat(result != null);

        assertThat(result.get(UID).asString() != null);
        assertThat(result.get(UID).asString().equalsIgnoreCase(ALICE));

        assertThat(result.get(PROFILE_FILENAME_KEY).asString() != null);
        assertThat(result.get(PROFILE_FILENAME_KEY).asString().equalsIgnoreCase(JPEG_PART_FILENAME));

        assertThat(result.get(PROFILE_CONTENT_KEY).asString() != null);
        assertThat(result.get(PROFILE_CONTENT_KEY).asString().equalsIgnoreCase(
                Base64url.encode(JPEG_CONTENT.getBytes())));

        assertThat(result.get(PROFILE_MIMETYPE_KEY).asString() != null);
        assertThat(result.get(PROFILE_MIMETYPE_KEY).asString().equalsIgnoreCase(JPEG_CONTENT_TYPE));
    }

    private void testNonMultiPartResult(JsonValue result) {
        assertThat(result != null);

        assertThat(result.get(UID).asString() != null);
        assertThat(result.get(UID).asString().equalsIgnoreCase(ALICE));

        assertThat(result.get(PROFILE_FILENAME_KEY) != null);
        assertThat(result.get(PROFILE_FILENAME_KEY).get("$ref").asString().equalsIgnoreCase(
                "cid:" + JPEG_PART_NAME + "#filename"));

        assertThat(result.get(PROFILE_CONTENT_KEY) != null);
        assertThat(result.get(PROFILE_CONTENT_KEY).get("$ref").asString().equalsIgnoreCase(
                "cid:" + JPEG_PART_NAME + "#content"));

        assertThat(result.get(PROFILE_MIMETYPE_KEY) != null);
        assertThat(result.get(PROFILE_MIMETYPE_KEY).get("$ref").asString().equalsIgnoreCase(
                "cid:" + JPEG_PART_NAME + "#mimetype"));
    }

    @Test
    public void testShouldPopulateReferencesWhenGetJsonActionContentIsCalled()
            throws ResourceException, IOException {
        //given
        request = newRequest();
        createMultiPartRequest(jsonBody);
        setUpRequestMock(request, REQUEST_CONTENT_TYPE);

        //when
        JsonValue result = HttpUtils.getJsonActionContent(request);

        //then
        testMultiPartResult(result);
    }

    @Test
    public void testShouldPopulateReferencesWhenGetJsonPatchContentIsCalled()
            throws ResourceException, IOException {
        //given
        request = newRequest();
        createMultiPartRequest(jsonPatchBody);
        setUpRequestMock(request, REQUEST_CONTENT_TYPE);

        ///when
        List<PatchOperation> result = HttpUtils.getJsonPatchContent(request);

        //then
        for (PatchOperation operation : result) {
            String field = operation.getField().leaf();
            if (field.equalsIgnoreCase(PROFILE_MIMETYPE_KEY)) {
                assertThat(operation.getValue().asString().equalsIgnoreCase(JPEG_CONTENT_TYPE));
            } else if (field.equalsIgnoreCase(PROFILE_CONTENT_KEY)) {
                assertThat(operation.getValue().asString().equals(
                        Base64url.encode(JPEG_CONTENT.getBytes())));
            } else if (field.equalsIgnoreCase(PROFILE_FILENAME_KEY)) {
                assertThat(operation.getValue().asString().equalsIgnoreCase(JPEG_PART_FILENAME));
            } else {
                throw new BadRequestException();
            }
        }
    }

    @Test
    public void testShouldPopulateReferencesWhenGetJsonContentIsCalled() throws ResourceException,
            IOException {
        //given
        request = newRequest();
        createMultiPartRequest(jsonBody);
        setUpRequestMock(request, REQUEST_CONTENT_TYPE);

        //when
        JsonValue result = HttpUtils.getJsonContent(request);

        //then
        testMultiPartResult(result);
    }

    @Test
    public void testShouldProcessARequestThatIsNotMultiPartWhenGetJsonActionContentIsCalled()
            throws ResourceException, IOException {
        //given
        request = newRequest();
        createRequest(jsonBody);
        setUpRequestMock(request, HttpUtils.MIME_TYPE_APPLICATION_JSON);

        //when
        JsonValue result = HttpUtils.getJsonActionContent(request);

        //then
        testNonMultiPartResult(result);
    }

    @Test
    public void testShouldProcessARequestThatIsNotMultiPartWhenGetJsonPatchContentIsCalled()
            throws ResourceException, IOException {
        //given
        request = newRequest();
        createRequest(jsonPatchBody);
        setUpRequestMock(request, HttpUtils.MIME_TYPE_APPLICATION_JSON);

        //when
        List<PatchOperation> result = HttpUtils.getJsonPatchContent(request);

        //then
        for (PatchOperation operation : result) {
            String field = operation.getField().leaf();
            if (field.equalsIgnoreCase(PROFILE_MIMETYPE_KEY)) {
                assertThat(operation.getValue().get("$ref").asString().equalsIgnoreCase(
                        "cid:" + JPEG_PART_NAME + "#mimetype"));
            } else if (field.equalsIgnoreCase(PROFILE_CONTENT_KEY)) {
                assertThat(operation.getValue().get("$ref").asString().equalsIgnoreCase(
                        "cid:" + JPEG_PART_NAME + "#content"));
            } else if (field.equalsIgnoreCase(PROFILE_FILENAME_KEY)) {
                assertThat(operation.getValue().get("$ref").asString().equalsIgnoreCase(
                        "cid:" + JPEG_PART_NAME + "#filename"));
            } else {
                throw new BadRequestException();
            }
        }
    }

    @Test
    public void testShouldProcessARequestThatIsNotMultiPartWhenGetJsonContentCalled()
            throws ResourceException, IOException {
        //given
        request = newRequest();
        createRequest(jsonBody);
        setUpRequestMock(request, HttpUtils.MIME_TYPE_APPLICATION_JSON);

        //when
        JsonValue result = HttpUtils.getJsonContent(request);

        //then
        testNonMultiPartResult(result);
    }

    @DataProvider
    public Object[][] ValidGetMethodContentTypeCombination() {
        return new Object[][]{
                { HttpUtils.METHOD_GET, HttpUtils.MIME_TYPE_APPLICATION_JSON, HttpUtils.MIME_TYPE_APPLICATION_JSON },
                { HttpUtils.METHOD_GET, HttpUtils.MIME_TYPE_TEXT_PLAIN, HttpUtils.MIME_TYPE_TEXT_PLAIN }
        };
    }

    private void setupPrepareResponseMocks(org.forgerock.http.Request httpRequest,
                                           final String method,
                                           final String contentType) throws URISyntaxException {
        httpRequest.setMethod(method);
        httpRequest.getHeaders().putSingle(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE, method);
        httpRequest.getUri().setQuery(HttpUtils.PARAM_MIME_TYPE + "=" + contentType);
    }

    @Test(dataProvider = "ValidGetMethodContentTypeCombination")
    public void testShouldSetResponseContentType(final String method, final String contentType, final String result)
            throws Exception {
        //given
        org.forgerock.http.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, method, contentType);

        //when
        Response httpResponse = HttpUtils.prepareResponse(httpRequest);

        //then
        assertThat(httpResponse.getHeaders())
                .hasSize(2)
                .contains(
                        entry(ContentTypeHeader.NAME, Collections.singletonList(
                                new ContentTypeHeader(result, HttpUtils.CHARACTER_ENCODING, null).toString())),
                        entry(HttpUtils.HEADER_CACHE_CONTROL, Collections.singletonList(HttpUtils.CACHE_CONTROL)));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testShouldThrowBadRequestExceptionOnInvalidContentTypeForGet()
            throws Exception {
        //given
        org.forgerock.http.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, HttpUtils.METHOD_GET, "unknown content type");

        //when
        try {
            HttpUtils.prepareResponse(httpRequest);
        } catch (BadRequestException e) {
            assertThat(e.getClass()).isEqualTo(BadRequestException.class);
            int a = 1;
            throw e;
        }
    }

    @DataProvider
    public Object[][] ValidPostMethodContentTypeCombination() {
        return new Object[][]{
                { HttpUtils.METHOD_POST, HttpUtils.MIME_TYPE_APPLICATION_JSON, HttpUtils.MIME_TYPE_APPLICATION_JSON },
                { HttpUtils.METHOD_POST, HttpUtils.MIME_TYPE_TEXT_PLAIN, HttpUtils.MIME_TYPE_APPLICATION_JSON },
                { HttpUtils.METHOD_POST, "Unknown content Type", HttpUtils.MIME_TYPE_APPLICATION_JSON }
        };
    }

    @Test(dataProvider = "ValidPostMethodContentTypeCombination")
    public void testShouldSetResponseContentTypeForPostMethod(final String method, final String contentType, final String result)
            throws Exception {
        //given
        org.forgerock.http.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, method, contentType);

        //when
        Response httpResponse = HttpUtils.prepareResponse(httpRequest);

        //then
        assertThat(httpResponse.getHeaders())
                .hasSize(2)
                .contains(
                        entry(ContentTypeHeader.NAME, Collections.singletonList(
                                new ContentTypeHeader(result, HttpUtils.CHARACTER_ENCODING, null).toString())),
                        entry(HttpUtils.HEADER_CACHE_CONTROL, Collections.singletonList(HttpUtils.CACHE_CONTROL)));
    }
}
