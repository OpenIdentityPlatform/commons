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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.http.HttpUtils.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.RequestType;
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

    private org.forgerock.http.protocol.Request request;

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

    private void setUpRequestMock(org.forgerock.http.protocol.Request request, String contentType)
            throws IOException {
        request.getHeaders().put(ContentTypeHeader.NAME, contentType);
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
    public Object[][] validGetMethodContentTypeCombination() {
        return new Object[][]{
            { HttpUtils.METHOD_GET, HttpUtils.MIME_TYPE_APPLICATION_JSON, HttpUtils.MIME_TYPE_APPLICATION_JSON },
            { HttpUtils.METHOD_GET, HttpUtils.MIME_TYPE_TEXT_PLAIN, HttpUtils.MIME_TYPE_TEXT_PLAIN }
        };
    }

    private void setupPrepareResponseMocks(org.forgerock.http.protocol.Request httpRequest,
                                           final String method,
                                           final String contentType) throws URISyntaxException {
        httpRequest.setMethod(method);
        httpRequest.getHeaders().put(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE, method);
        httpRequest.getUri().setQuery(HttpUtils.PARAM_MIME_TYPE + "=" + contentType);
    }

    @Test(dataProvider = "validGetMethodContentTypeCombination")
    public void testShouldSetResponseContentType(final String method, final String contentType, final String result)
            throws Exception {
        //given
        org.forgerock.http.protocol.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, method, contentType);

        //when
        Response httpResponse = HttpUtils.prepareResponse(httpRequest);

        //then
        assertThat(httpResponse.getHeaders())
                .contains(
                        entry(ContentTypeHeader.NAME,
                                new ContentTypeHeader(result, CHARACTER_ENCODING, null)),
                        entry(HEADER_CACHE_CONTROL, new GenericHeader(HEADER_CACHE_CONTROL, CACHE_CONTROL)));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testShouldThrowBadRequestExceptionOnInvalidContentTypeForGet()
            throws Exception {
        //given
        org.forgerock.http.protocol.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, HttpUtils.METHOD_GET, "unknown content type");

        //when
        try {
            HttpUtils.prepareResponse(httpRequest);
        } catch (BadRequestException e) {
            assertThat(e.getClass()).isEqualTo(BadRequestException.class);
            throw e;
        }
    }

    @DataProvider
    public Object[][] validPostMethodContentTypeCombination() {
        return new Object[][]{
            { HttpUtils.METHOD_POST, HttpUtils.MIME_TYPE_APPLICATION_JSON, HttpUtils.MIME_TYPE_APPLICATION_JSON },
            { HttpUtils.METHOD_POST, HttpUtils.MIME_TYPE_TEXT_PLAIN, HttpUtils.MIME_TYPE_APPLICATION_JSON },
            { HttpUtils.METHOD_POST, "Unknown content Type", HttpUtils.MIME_TYPE_APPLICATION_JSON }
        };
    }

    @Test(dataProvider = "validPostMethodContentTypeCombination")
    public void testShouldSetResponseContentTypeForPostMethod(final String method, final String contentType,
            final String result) throws Exception {
        //given
        org.forgerock.http.protocol.Request httpRequest = newRequest();

        setupPrepareResponseMocks(httpRequest, method, contentType);

        //when
        Response httpResponse = HttpUtils.prepareResponse(httpRequest);

        //then
        assertThat(httpResponse.getHeaders())
                .contains(
                        entry(ContentTypeHeader.NAME,
                                new ContentTypeHeader(result, CHARACTER_ENCODING, null)),
                        entry(HEADER_CACHE_CONTROL, new GenericHeader(HEADER_CACHE_CONTROL, CACHE_CONTROL)));
    }

    @DataProvider
    public Object[][] badJsonContent() {
        return new Object[][] {
            { "" }, // No JSON content
            { "{{{" }, // Invalid JSON content
            { "{ \"test\" : \"value\" }garbage" } // trailing garbage JSON content
        };
    }

    @Test(dataProvider = "badJsonContent", expectedExceptions = BadRequestException.class)
    public void testBadJsonContent(final String badContent) throws Exception {
        // given
        request = newRequest();
        createRequest(badContent);
        setUpRequestMock(request, HttpUtils.MIME_TYPE_APPLICATION_JSON);

        // when
        try {
            HttpUtils.getJsonContent(request);
        } catch (BadRequestException e) {
            assertThat(e.getClass()).isEqualTo(BadRequestException.class);
            throw e;
        }
    }

    /* Test cases for PUT with If-None-Match: 1 - should generate BadRequestException */
    @DataProvider
    public Object[][] requestToException() {
        return new Object[][] {
            { putRequest("1.0", "1", null) },
            { putRequest("2.0", "1", null) },
            { putRequest(null, "1", null) },
            { postRequestNoActionParamDefactoCreateUnsupportedVersion() }
        };
    }

    /**
     * CREST-346: Supplying an If-None-Match: 1 (non-null, non-ETAG_ANY) should result in a BadRequestException.
     *
     * @param request the request
     * @throws ResourceException on illegal request
     */
    @Test(dataProvider = "requestToException", expectedExceptions = BadRequestException.class)
    public void testDetermineRequestType(Request request) throws ResourceException {
        determineRequestType(request);
    }

    @DataProvider
    public Object[][] requestToRequestType() {
        return new Object[][] {
            { RequestType.READ, getRequestNoParams() },
            { RequestType.QUERY, getRequestQueryParam() },
            { RequestType.DELETE, deleteRequest() },
            { RequestType.PATCH, patchRequest() },
            { RequestType.ACTION, postRequestNonCreateActionParam() },
            { RequestType.CREATE, postRequestCreateActionParam() },
            { RequestType.CREATE, postRequestNoActionParamDefactoCreateSupportedVersion() },
            { RequestType.CREATE, postRequestNoActionParamDefaultVersion() },

            // Protocol 1.0 PUT tests
            { RequestType.CREATE, putRequest("1.0", HttpUtils.ETAG_ANY, null) },
            { RequestType.UPDATE, putRequest("1.0", null, HttpUtils.ETAG_ANY) },
            { RequestType.UPDATE, putRequest("1.0", null, "1") },
            { RequestType.UPDATE, putRequest("1.0", null, null) },
            // Protocol 2.0 PUT tests
            { RequestType.CREATE, putRequest("2.0", HttpUtils.ETAG_ANY, null) },
            { RequestType.UPDATE, putRequest("2.0", null, "1") },
            { RequestType.UPDATE, putRequest("2.0", null, HttpUtils.ETAG_ANY) },
            // This changes in protocol=2: A PUT with neither If-None-Match/If-Match header
            // is an "upsert" -- it starts as a CREATE, but if the create fails because the
            // resource exists, an UPDATE will be attempted (CREST-100).
            // see also RequestRunner#visitCreateRequest
            { RequestType.CREATE, putRequest("2.0", null, null) },

            // No protocol specified == version 2.0 behavior
            { RequestType.CREATE, putRequest(null, HttpUtils.ETAG_ANY, null) },
            { RequestType.UPDATE, putRequest(null, null, "1") },
            { RequestType.UPDATE, putRequest(null, null, HttpUtils.ETAG_ANY) },
            { RequestType.CREATE, putRequest(null, null, null) }
        };
    }

    @Test(dataProvider = "requestToRequestType")
    public void testDetermineRequestType(RequestType requestType, Request request) throws ResourceException {
        assertThat(determineRequestType(request)).isEqualTo(requestType);
    }

    private Request getRequestNoParams() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_GET);
        return request;
    }

    private Request getRequestQueryParam() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_GET);
        request.setUri(URI.create("?" + HttpUtils.PARAM_QUERY_FILTER + "=true"));
        return request;
    }

    private Request deleteRequest() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_DELETE);
        return request;
    }

    private Request patchRequest() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_PATCH);
        return request;
    }

    private Request postRequestNonCreateActionParam() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_POST);
        request.setUri(URI.create("?" + HttpUtils.PARAM_ACTION + "=test"));
        return request;
    }

    private Request postRequestNoActionParamDefactoCreateSupportedVersion() {
        Request request = newRequest().setMethod(HttpUtils.METHOD_POST);
        request.getHeaders().add(new AcceptApiVersionHeader(version(2, 1), version(1)));
        return request;
    }

    private Request postRequestNoActionParamDefactoCreateUnsupportedVersion() {
        Request request = newRequest().setMethod(HttpUtils.METHOD_POST);
        request.getHeaders().add(new AcceptApiVersionHeader(version(2, 0), version(1)));
        return request;
    }

    private Request postRequestNoActionParamDefaultVersion() {
        Request request = newRequest().setMethod(HttpUtils.METHOD_POST);
        return request;
    }

    private Request postRequestCreateActionParam() {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_POST);
        request.setUri(URI.create("?" + HttpUtils.PARAM_ACTION + "=" + ActionRequest.ACTION_ID_CREATE));
        return request;
    }

    private Request putRequest(String protocolVersion, String ifNoneMatch, String ifMatch) {
        Request request = newRequest()
                .setMethod(HttpUtils.METHOD_PUT);
        if (protocolVersion != null) {
            request.getHeaders().put(AcceptApiVersionHeader.NAME, "protocol=" + protocolVersion + ",resource=1.0");
        }
        if (ifNoneMatch != null) {
            request.getHeaders().put(HttpUtils.HEADER_IF_NONE_MATCH, ifNoneMatch);
        }
        if (ifMatch != null) {
            request.getHeaders().put(HttpUtils.HEADER_IF_MATCH, ifMatch);
        }
        return request;
    }

    private Request newRequest() {
        Request request = new Request();
        request.setUri(URI.create(""));
        return request;
    }
}
