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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.http.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.http.io.IO.nullOutputStream;
import static org.forgerock.http.protocol.Entity.APPLICATION_JSON_CHARSET_UTF_8;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.BranchingInputStream;
import org.forgerock.http.io.IO;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class EntityTest {
    private static final String INVALID_JSON = "invalid json";
    private static final String JSON_CONTENT1 = singleQuotesToDouble("{'a':1,'b':2}");
    private static final String JSON_CONTENT2 = singleQuotesToDouble("{'c':3,'d':4}");

    private Entity entity;
    private Request message;
    private BranchingInputStream mockJsonContent1;
    private BranchingInputStream mockJsonContent2;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        message = new Request();
        entity = message.getEntity();
        mockJsonContent1 = mockContent(JSON_CONTENT1);
        mockJsonContent2 = mockContent(JSON_CONTENT2);
    }

    @Test
    public void entityIsEmptyByDefault() throws Exception {
        assertThat(entity.getRawContentInputStream().available()).isEqualTo(0);
        assertThat(entity.newDecodedContentInputStream().available()).isEqualTo(0);
        assertThat(entity.newDecodedContentReader(null).readLine()).isNull();
        assertThat(entity.getBytes()).isEmpty();
        assertThat(entity.getString()).isEmpty();
        assertThat(entity.toString()).isEmpty();
        assertThat(entity.isDecodedContentEmpty()).isTrue();
        entity.push();
        assertThat(entity.getRawContentInputStream().available()).isEqualTo(0);
        entity.pop();
        entity.close();
    }

    @Test
    public void getBytes() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.getBytes()).isEqualTo(bytes(JSON_CONTENT1));
        assertThat(mockJsonContent1.available()).isEqualTo(JSON_CONTENT1.length());
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        verify(mockJsonContent1, never()).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getJson() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.getJson()).isInstanceOf(Map.class);
        final Map<String, Integer> jsonEntity = ((Map<String, Integer>) entity.getJson());

        assertThat(jsonEntity).hasSize(2);
        assertThat(jsonEntity).contains(entry("a", 1), entry("b", 2));

        verify(mockJsonContent1, never()).close();
    }

    @Test(expectedExceptions = IOException.class)
    public void getJsonWhenEntityContainsInvalidJsonThrowsIOException() throws Exception {
        mockJsonContent1 = mockContent(INVALID_JSON);
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        try {
            entity.getJson();
        } finally {
            // The stream should be untouched despite the error.
            assertThat(mockJsonContent1.available()).isEqualTo(INVALID_JSON.length());
            verify(mockJsonContent1, never()).close();
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void getJsonWhenEntityIsEmpty() throws Exception {
        entity.getJson();
    }

    @Test
    public void setNullAsJsonContent() throws Exception {
        entity.setJson(null);
        assertThat(entity.getJson()).isNull();
    }

    @Test
    public void getPushAndPop() throws Exception {
        /*
         * We cannot use the mock content here because it is bypassed during
         * calling getParent() on the stream.
         */
        final BranchingInputStream content = IO.newBranchingInputStream(bytes(JSON_CONTENT1));
        entity.setRawContentInputStream(content);
        entity.push();
        assertThat(entity.getRawContentInputStream()).isNotSameAs(content);
        entity.getRawContentInputStream().close();
        assertThat(entity.getRawContentInputStream()).isNotSameAs(content);
        entity.pop();
        assertThat(entity.getRawContentInputStream()).isSameAs(content);
        assertThat(content.available()).isEqualTo(JSON_CONTENT1.length());
    }

    @Test
    public void getRawInputStream() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.getRawContentInputStream()).isSameAs(mockJsonContent1);
        assertThat(mockJsonContent1.available()).isEqualTo(JSON_CONTENT1.length());
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        verify(mockJsonContent1, never()).close();
    }

    @Test
    public void getString() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.getString()).isEqualTo(JSON_CONTENT1);
        assertThat(mockJsonContent1.available()).isEqualTo(JSON_CONTENT1.length());
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        verify(mockJsonContent1, never()).close();
    }

    @Test
    public void setBytes() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setBytes(bytes(JSON_CONTENT2));
        assertThatContentIsJsonContent2();
        assertThatContentLengthHeaderIsPresentForJsonContent2();
        assertThatContentyTypeHeaderIsNotPresent();
    }

    @Test
    public void setBytesNullBecomesEmptyStream() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setBytes(null);
        assertThat(entity.getRawContentInputStream().available()).isEqualTo(0);
    }

    @Test
    public void setJson() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        Map<String, Object> jsonEntity = new LinkedHashMap<>();
        jsonEntity.put("c", 3);
        jsonEntity.put("d", 4);
        entity.setJson(jsonEntity);
        assertThatContentIsJsonContent2();
        assertThatContentLengthHeaderIsPresentForJsonContent2();
        assertThatContentTypeHeaderIsPresent();
    }

    @Test(enabled = false)
    public void setJsonAsBean() throws Exception {
        class MyBean {
            private String name;

            MyBean(final String mName) {
                name = mName;
            }

            @SuppressWarnings("unused")
            public String getName() {
                return name;
            }

            @SuppressWarnings("unused")
            public void setName(String name) {
                this.name = name;
            }
        }
        final String json = singleQuotesToDouble("{'name':'Jackson'}");
        final MyBean mybean = new MyBean("Jackson");
        entity.setJson(mybean);
        assertThat(entity.getJson()).isSameAs(mybean); // Shouldn't this to be the json representation ?
        assertThat(entity.getString()).isEqualTo(json);
    }

    @Test
    public void setJsonNullIsJsonNull() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setJson(null);
        assertThat(entity.getRawContentInputStream().available()).isEqualTo("null".length());
    }

    @Test
    public void setRawInputStream() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setRawContentInputStream(mockJsonContent2);
        assertThatContentIsJsonContent2();
        assertThatContentLengthHeaderIsNotPresent();
        assertThatContentyTypeHeaderIsNotPresent();
    }

    @Test
    public void setRawInputStreamNullBecomesEmptyStream() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setRawContentInputStream(null);
        assertThat(entity.getRawContentInputStream().available()).isEqualTo(0);
    }

    @Test
    public void setString() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setString(JSON_CONTENT2);
        assertThatContentIsJsonContent2();
        assertThatContentLengthHeaderIsPresentForJsonContent2();
        assertThatContentyTypeHeaderIsNotPresent();
    }

    @Test
    public void setStringNullBecomesEmptyStream() throws Exception {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThatContentIsJsonContent1();
        entity.setString(null);
        assertThat(entity.getRawContentInputStream().available()).isEqualTo(0);
    }

    @Test
    public void shouldParseJsonObjectReturnsLinkedHashMap() throws IOException {
        entity.setRawContentInputStream(mockJsonContent1);
        assertThat(entity.getJson()).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    public void shouldMarkTheEntityAsEmpty() throws Exception {
        assertThat(entity.isDecodedContentEmpty()).isTrue();
        entity.setJson(new HashMap<String, Object>());
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        entity.setEmpty();
        assertThat(entity.isDecodedContentEmpty()).isTrue();
    }

    @Test
    public void shouldNotChangeEntityContentWhenCallingIsEmpty() throws Exception {
        entity.setJson(JSON_CONTENT1);
        assertThat(entity.isDecodedContentEmpty()).isFalse();
        assertThat(entity.getJson()).isSameAs(JSON_CONTENT1);
    }

    @Test
    public void shouldBeEmptyEvenInPushMode() throws Exception {
        assertThat(entity.isDecodedContentEmpty()).isTrue();
        entity.push();
        try {
            assertThat(entity.isDecodedContentEmpty()).isTrue();
        } finally {
            entity.pop();
        }
    }

    @Test
    public void testDefensiveCopyIsNotAffectedWhenOriginalEntityIsConsumed() throws Exception {
        entity.setString("Hello");
        Entity copy = new Entity(new Request(), entity);

        entity.copyDecodedContentTo(nullOutputStream());

        assertThat(copy.getString()).isEqualTo("Hello");
    }

    @Test
    public void testDefensiveCopyIsNotAffectedWhenOriginalEntityIsChanged() throws Exception {
        entity.setString("Hello");
        Entity copy = new Entity(new Request(), entity);

        entity.setString("Bonjour");

        assertThat(copy.getString()).isEqualTo("Hello");
    }

    @Test
    public void testDefensiveCopyIsNotAffectedWhenOriginalEntityIsChanged2() throws Exception {
        // ByteArrayBranchingStream is not affected by close(), for a real use case we must wrap a real stream
        entity.setRawContentInputStream(branchingStream(bytes("Hello")));
        Entity copy = new Entity(new Request(), entity);

        entity.close();

        assertThat(copy.getString()).isEqualTo("Hello");
    }

    @Test
    public void testReadingFromDefensiveCopyDoesNotAffectOriginalEntity() throws Exception {
        entity.setString("Hello");
        Entity copy = new Entity(new Request(), entity);

        copy.copyDecodedContentTo(nullOutputStream());

        assertThat(entity.getString()).isEqualTo("Hello");
    }

    @Test
    public void testClosingDefensiveCopyDoesNotAffectOriginalEntity() throws Exception {
        entity.setRawContentInputStream(branchingStream(bytes("Hello")));
        Entity copy = new Entity(new Request(), entity);

        copy.close();

        assertThat(entity.getString()).isEqualTo("Hello");
    }

    private static BranchingInputStream branchingStream(final byte[] bytes) throws UnsupportedEncodingException {
        return IO.newBranchingInputStream(new ByteArrayInputStream(bytes), IO.newTemporaryStorage());
    }

    @SuppressWarnings("unchecked")
    private void assertThatContentIsJsonContent1() throws IOException {
        assertThat(entity.getString()).isEqualTo(JSON_CONTENT1);
        assertThat(entity.getBytes()).isEqualTo(bytes(JSON_CONTENT1));

        assertThat(entity.getJson()).isInstanceOf(LinkedHashMap.class);
        final Map<String, Integer> jsonEntity = ((LinkedHashMap<String, Integer>) entity.getJson());

        assertThat(jsonEntity).hasSize(2);
        assertThat(jsonEntity).contains(entry("a", 1), entry("b", 2));
        assertThat(mockJsonContent1.available()).isEqualTo(JSON_CONTENT1.length());
    }

    @SuppressWarnings("unchecked")
    private void assertThatContentIsJsonContent2() throws IOException {
        assertThat(entity.getRawContentInputStream()).isNotSameAs(mockJsonContent1);
        assertThat(entity.getBytes()).isEqualTo(bytes(JSON_CONTENT2));

        assertThat(entity.getJson()).isInstanceOf(LinkedHashMap.class);
        final Map<String, Integer> jsonEntity = ((LinkedHashMap<String, Integer>) entity.getJson());

        assertThat(jsonEntity).hasSize(2);
        assertThat(jsonEntity).contains(entry("c", 3), entry("d", 4));

        assertThat(entity.getRawContentInputStream().available()).isEqualTo(JSON_CONTENT2.length());
        verify(mockJsonContent1).close();
    }

    private void assertThatContentLengthHeaderIsNotPresent() {
        assertThat(message.getHeaders().get(ContentLengthHeader.NAME)).isNull();
    }

    private void assertThatContentLengthHeaderIsPresentForJsonContent2() {
        assertThat(message.getHeaders()).containsKey(ContentLengthHeader.NAME);
        assertThat(message.getHeaders().get(ContentLengthHeader.NAME).getValues())
                .containsOnly(String.valueOf(JSON_CONTENT2.length()));
    }

    private void assertThatContentTypeHeaderIsPresent() {
        assertThat(message.getHeaders()).containsKey(ContentTypeHeader.NAME);
        assertThat(message.getHeaders().get(ContentTypeHeader.NAME).getValues())
                .containsOnly(APPLICATION_JSON_CHARSET_UTF_8);
    }

    private void assertThatContentyTypeHeaderIsNotPresent() {
        assertThat(message.getHeaders().get(ContentTypeHeader.NAME)).isNull();
    }

    private byte[] bytes(final String s) throws UnsupportedEncodingException {
        return s.getBytes("UTF-8");
    }

    private BranchingInputStream mockContent(final byte[] bytes) throws UnsupportedEncodingException {
        return mock(BranchingInputStream.class, delegatesTo(branchingStream(bytes)));
    }

    /**
     * Input streams are painful to mock, so we'll use the new delegating answer
     * support in 1.9.5 to monitor and proxy calls to a real input stream.
     */
    private BranchingInputStream mockContent(final String content)
            throws UnsupportedEncodingException {
        return mockContent(content.getBytes("UTF-8"));
    }

    /** Remove single quotes from a string. */
    private static String singleQuotesToDouble(final String value) {
        return value.replace('\'', '\"');
    }
}
