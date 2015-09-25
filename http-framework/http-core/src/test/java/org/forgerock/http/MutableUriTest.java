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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.MutableUri.uri;
import static org.forgerock.http.util.Uris.urlFormEncode;

import java.net.URI;
import java.net.URISyntaxException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class MutableUriTest {

    @Test(expectedExceptions = URISyntaxException.class)
    public void shouldFailWithInvalidUri() throws Exception {
        new MutableUri("http://<<servername>>:8080");
    }

    // Test for un-encoded values
    // ----------------------------------------------------------

    @Test
    public void shouldUpdateScheme() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setScheme("https");
        assertThat(uri).isEqualTo(uri("https://www.example.com"));
        assertThat(uri.getScheme()).isEqualTo("https");
    }

    @Test
    public void shouldUpdateHost() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setHost("openig.forgerock.org");
        assertThat(uri).isEqualTo(uri("http://openig.forgerock.org"));
        assertThat(uri.getHost()).isEqualTo("openig.forgerock.org");
    }

    @Test
    public void shouldUpdatePort() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setPort(8080);
        assertThat(uri).isEqualTo(uri("http://www.example.com:8080"));
        assertThat(uri.getPort()).isEqualTo(8080);
    }

    @Test
    public void shouldRemovePort() throws Exception {
        MutableUri uri = uri("http://www.example.com:8080");
        uri.setPort(-1);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
        assertThat(uri.getPort()).isEqualTo(-1);
    }

    @Test
    public void shouldAddUserInfo() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setUserInfo("bjensen:s3cr3t");
        assertThat(uri).isEqualTo(uri("http://bjensen:s3cr3t@www.example.com"));
        assertThat(uri.getUserInfo()).isEqualTo("bjensen:s3cr3t");
    }

    @Test
    public void shouldModifyUserInfo() throws Exception {
        MutableUri uri = uri("http://bjensen:s3cr3t@www.example.com");
        uri.setUserInfo("guillaume:password");
        assertThat(uri).isEqualTo(uri("http://guillaume:password@www.example.com"));
        assertThat(uri.getUserInfo()).isEqualTo("guillaume:password");
    }

    @Test
    public void shouldRemoveUserInfo() throws Exception {
        MutableUri uri = uri("http://bjensen:s3cr3t@www.example.com");
        uri.setUserInfo(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
        assertThat(uri.getUserInfo()).isNull();
    }

    @Test
    public void shouldAddPath() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setPath("/openig space/fr");
        assertThat(uri).isEqualTo(uri("http://www.example.com/openig%20space/fr"));
        assertThat(uri.getPath()).isEqualTo("/openig space/fr");
    }

    @Test
    public void shouldModifyPath() throws Exception {
        MutableUri uri = uri("http://www.example.com/openig");
        uri.setPath("/forgerock");
        assertThat(uri).isEqualTo(uri("http://www.example.com/forgerock"));
        assertThat(uri.getPath()).isEqualTo("/forgerock");
    }

    @Test
    public void shouldRemovePath() throws Exception {
        MutableUri uri = uri("http://www.example.com/openig");
        uri.setPath(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
        // Note: because we rebuild the full URL at each modification, it seems the underlying URI find an empty path
        // instead of a null pah
        assertThat(uri.getPath()).isNullOrEmpty();
    }

    @Test
    public void shouldAddQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setQuery("one=two three");
        assertThat(uri).isEqualTo(uri("http://www.example.com?one=two%20three"));
        assertThat(uri.getQuery()).isEqualTo("one=two three");
    }

    @Test
    public void shouldModifyQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com?one=two%20three");
        uri.setQuery("a=b");
        assertThat(uri).isEqualTo(uri("http://www.example.com?a=b"));
        assertThat(uri.getQuery()).isEqualTo("a=b");
    }

    @Test
    public void shouldRemoveQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com?a=b");
        uri.setQuery(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
        assertThat(uri.getQuery()).isNull();
    }

    @Test
    public void shouldAddFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setFragment("marker one");
        assertThat(uri).isEqualTo(uri("http://www.example.com#marker%20one"));
        assertThat(uri.getFragment()).isEqualTo("marker one");
    }

    @Test
    public void shouldModifyFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com#other");
        uri.setFragment("marker one");
        assertThat(uri).isEqualTo(uri("http://www.example.com#marker%20one"));
        assertThat(uri.getFragment()).isEqualTo("marker one");
    }

    @Test
    public void shouldRemoveFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com#marker");
        uri.setFragment(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
        assertThat(uri.getFragment()).isNull();
    }

    // Test for encoded values (URL encoded)
    // ----------------------------------------------------------

    @Test
    public void shouldAddRawUserInfo() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawUserInfo("bjen%20sen:s3c%3Dr3t");
        assertThat(uri).isEqualTo(uri("http://bjen%20sen:s3c%3Dr3t@www.example.com"));
    }

    @Test
    public void shouldRemoveRawUserInfo() throws Exception {
        MutableUri uri = uri("http://bjensen:s3cr3t@www.example.com");
        uri.setRawUserInfo(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
    }

    @Test
    public void shouldAddRawPath() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawPath("/openig%20space/fr");
        assertThat(uri).isEqualTo(uri("http://www.example.com/openig%20space/fr"));
    }

    @Test
    public void shouldRemoveRawPath() throws Exception {
        MutableUri uri = uri("http://www.example.com/openig");
        uri.setRawPath(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
    }

    @Test
    public void shouldAddRawQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawQuery("one=two%20three");
        assertThat(uri).isEqualTo(uri("http://www.example.com?one=two%20three"));
    }

    @Test
    public void shouldRemoveRawQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com?a=b");
        uri.setRawQuery(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
    }

    @Test
    public void shouldAddRawFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawFragment("marker%20one");
        assertThat(uri).isEqualTo(uri("http://www.example.com#marker%20one"));
    }

    @Test
    public void shouldRemoveRawFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com#marker");
        uri.setRawFragment(null);
        assertThat(uri).isEqualTo(uri("http://www.example.com"));
    }

    // Test for getter (raw and normal)
    // ----------------------------------------------------------

    @Test
    public void testGetters() throws Exception {
        MutableUri uri = uri("http://my%20user:pass%3Fword@www.example.com:80/path%20space/fr?x=%3D#marker%20one");
        assertThat(uri.getScheme()).isEqualTo("http");
        assertThat(uri.getUserInfo()).isEqualTo("my user:pass?word");
        assertThat(uri.getRawUserInfo()).isEqualTo("my%20user:pass%3Fword");
        assertThat(uri.getHost()).isEqualTo("www.example.com");
        assertThat(uri.getPort()).isEqualTo(80);
        assertThat(uri.getAuthority()).isEqualTo("my user:pass?word@www.example.com:80");
        assertThat(uri.getRawAuthority()).isEqualTo("my%20user:pass%3Fword@www.example.com:80");
        assertThat(uri.getPath()).isEqualTo("/path space/fr");
        assertThat(uri.getRawPath()).isEqualTo("/path%20space/fr");
        assertThat(uri.getQuery()).isEqualTo("x==");
        assertThat(uri.getRawQuery()).isEqualTo("x=%3D");
        assertThat(uri.getFragment()).isEqualTo("marker one");
        assertThat(uri.getRawFragment()).isEqualTo("marker%20one");
    }

    // Other methods
    // ---------------------------------------

    @Test(enabled = false, expectedExceptions = IllegalArgumentException.class) // SEE OPENIG-468
    public void shouldFailToRebaseWithEmptyUri() throws Exception {
        final MutableUri uri = uri("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        uri.rebase(URI.create(""));
    }

    @Test
    public void shouldRebaseSchemeHostAndPort() throws Exception {
        MutableUri uri = uri("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        uri.rebase(new URI("http://www.example.com:8080"));
        assertThat(uri.toString())
                .isEqualTo("http://www.example.com:8080/all/good/things?come=to&those=who#breakdance");
    }

    @Test
    public void shouldRebaseSchemeHostAndPortAndIgnoringOtherElements() throws Exception {
        MutableUri uri = uri("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        uri.rebase(new URI("http://www.example.com:8080/mypath?a=b#marker"));
        assertThat(uri.toString())
                .isEqualTo("http://www.example.com:8080/all/good/things?come=to&those=who#breakdance");
    }

    // Tests for correct encoding of clear values with reserved characters in URI components
    // --------------------------------------------------------------------------------------

    public static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final String DIGIT = "0123456789";
    public static final String GEN_DELIMS = ":/?#[]@";
    public static final String SUB_DELIMS = "!$&'()*+,;=";
    public static final String RESERVED = GEN_DELIMS + SUB_DELIMS;
    public static final String UNRESERVED = ALPHA + DIGIT + "-._~";
    public static final String PCHAR = UNRESERVED + SUB_DELIMS + ":@"; // Does not have %encoded values
    public static final String QUERY = PCHAR + "/?";
    public static final String FRAGMENT = PCHAR + "/?";
    public static final String USERINFO = UNRESERVED + SUB_DELIMS + ":";
    public static final String PATH = PCHAR;

    public static Object[][] arrayOf(String set) {
        char[] chars = set.toCharArray();
        int i = 0;
        Object[][] arr = new Object[chars.length][1];
        for (char c : chars) {
            arr[i++][0] = Character.toString(c);
        }
        return arr;
    }

    @DataProvider
    public static Object[][] queryChars() {
        return arrayOf(QUERY);
    }

    @Test(dataProvider = "queryChars")
    public void shouldNotEncodeLegalQueryChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setQuery(character);
        assertThat(uri.getQuery()).isEqualTo(character);
        assertThat(uri.getRawQuery()).isEqualTo(character);
    }

    @Test(dataProvider = "illegalQueryAndFragmentChars")
    public void shouldEncodeIllegalQueryChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setQuery(character);
        assertThat(uri.getQuery()).isEqualTo(character);
        assertThat(uri.getRawQuery()).isEqualTo(urlFormEncode(character));
    }

    @DataProvider
    public static Object[][] pathChars() {
        return arrayOf(PATH);
    }

    @Test(dataProvider = "pathChars")
    public void shouldNotEncodeLegalPathChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setPath("/" + character);
        assertThat(uri.getPath()).isEqualTo("/" + character);
        assertThat(uri.getRawPath()).isEqualTo("/" + character);
        assertThat(uri.getPathElements()).containsExactly(character);
    }

    @DataProvider
    public static Object[][] illegalPathChars() {
        return arrayOf(GEN_DELIMS.replace(":", "").replace("@", "").replace("/", ""));
    }

    @Test(dataProvider = "illegalPathChars")
    public void shouldEncodeIllegalPathChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setPath("/" + character);
        assertThat(uri.getPath()).isEqualTo("/" + character);
        assertThat(uri.getRawPath()).isEqualTo("/" + urlFormEncode(character));
        assertThat(uri.getPathElements().toString()).isEqualTo(urlFormEncode(character));
    }

    @DataProvider
    public static Object[][] fragmentChars() {
        return arrayOf(FRAGMENT);
    }

    @Test(dataProvider = "fragmentChars")
    public void shouldNotEncodeLegalFragmentChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setFragment(character);
        assertThat(uri.getFragment()).isEqualTo(character);
        assertThat(uri.getRawFragment()).isEqualTo(character);
    }

    @DataProvider
    public static Object[][] illegalQueryAndFragmentChars() {
        return arrayOf(GEN_DELIMS.replace(":", "")
                                 .replace("@", "")
                                 .replace("/", "")
                                 .replace("?", "")
                                 .replace("[", "") // See URI javadoc
                                 .replace("]", ""));
    }

    @Test(dataProvider = "illegalQueryAndFragmentChars")
    public void shouldEncodeIllegalFragmentChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setFragment(character);
        assertThat(uri.getFragment()).isEqualTo(character);
        assertThat(uri.getRawFragment()).isEqualTo(urlFormEncode(character));
    }

    @DataProvider
    public static Object[][] userInfoChars() {
        return arrayOf(USERINFO);
    }

    @Test(dataProvider = "userInfoChars")
    public void shouldNotEncodeLegalUserInfoChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setUserInfo(character);
        assertThat(uri.getUserInfo()).isEqualTo(character);
        assertThat(uri.getRawUserInfo()).isEqualTo(character);
    }

    @DataProvider
    public static Object[][] illegalUserInfoChars() {
        return arrayOf(GEN_DELIMS.replace(":", ""));
    }

    @Test(dataProvider = "illegalUserInfoChars")
    public void shouldEncodeIllegalUserInfoChars(final String character) throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setUserInfo(character);
        assertThat(uri.getUserInfo()).isEqualTo(character);
        assertThat(uri.getRawUserInfo()).isEqualTo(urlFormEncode(character));
    }

    @Test
    public void shouldNotDecodePercentEncodedValuesInRawUserInfo() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawUserInfo("%3A");
        assertThat(uri.getUserInfo()).isEqualTo(":");
        assertThat(uri.getRawUserInfo()).isEqualTo("%3A");
    }

    @Test
    public void shouldEncodePercentInUserInfo() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setUserInfo("%");
        assertThat(uri.getUserInfo()).isEqualTo("%");
        assertThat(uri.getRawUserInfo()).isEqualTo("%25");
    }

    @Test
    public void shouldNotDecodePercentEncodedValuesInRawPath() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawPath("/%3A");
        assertThat(uri.getPath()).isEqualTo("/:");
        assertThat(uri.getRawPath()).isEqualTo("/%3A");
    }

    @Test
    public void shouldEncodePercentInPath() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setPath("/%");
        assertThat(uri.getPath()).isEqualTo("/%");
        assertThat(uri.getRawPath()).isEqualTo("/%25");
    }

    @Test
    public void shouldNotDecodePercentEncodedValuesInRawQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawQuery("%3A");
        assertThat(uri.getQuery()).isEqualTo(":");
        assertThat(uri.getRawQuery()).isEqualTo("%3A");
    }

    @Test
    public void shouldEncodePercentInQuery() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setQuery("%");
        assertThat(uri.getQuery()).isEqualTo("%");
        assertThat(uri.getRawQuery()).isEqualTo("%25");
    }

    @Test
    public void shouldNotDecodePercentEncodedValuesInRawFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setRawFragment("%3A");
        assertThat(uri.getFragment()).isEqualTo(":");
        assertThat(uri.getRawFragment()).isEqualTo("%3A");
    }

    @Test
    public void shouldEncodePercentInFragment() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        uri.setFragment("%");
        assertThat(uri.getFragment()).isEqualTo("%");
        assertThat(uri.getRawFragment()).isEqualTo("%25");
    }

    @Test
    public void shouldNotDecodeQueryParamsWhenUpdatingAnotherField() throws Exception {
        MutableUri uri = uri("http://www.example.com/abc?param=%2B");
        uri.setHost("internal");
        assertThat(uri.asURI()).isEqualTo(new URI("http://internal/abc?param=%2B"));
        assertThat(uri.toString()).isEqualTo("http://internal/abc?param=%2B");
        assertThat(uri.toASCIIString()).isEqualTo("http://internal/abc?param=%2B");
        assertThat(uri.getQuery()).isEqualTo("param=+");
        assertThat(uri.getRawQuery()).isEqualTo("param=%2B");
    }

    @Test
    public void shouldRelativize() throws Exception {
        MutableUri uri = uri("http://www.example.com/a%20b/");
        MutableUri relativized = uri.relativize(uri("http://www.example.com/a%20b/c%3Dd"));
        assertThat(relativized).isEqualTo(uri("c%3Dd"));
        assertThat(relativized.getPathElements().toString()).isEqualTo("c=d");
    }

    @Test
    public void shouldResolveKeepingTheExistingPath() throws Exception {
        MutableUri uri = uri("http://www.example.com/a%20b/");
        MutableUri resolved = uri.resolve(uri("c%3Dd"));
        assertThat(resolved).isEqualTo(uri("http://www.example.com/a%20b/c%3Dd"));
        assertThat(resolved.getPathElements().toString()).isEqualTo("a%20b/c=d");
    }

    @Test
    public void shouldResolveStrippingTheExistingPath() throws Exception {
        MutableUri uri = uri("http://www.example.com/a%20b/");
        MutableUri resolved = uri.resolve(uri("/c%3Dd"));
        assertThat(resolved).isEqualTo(uri("http://www.example.com/c%3Dd"));
        assertThat(resolved.getPathElements().toString()).isEqualTo("c=d");
    }

    @Test
    public void shouldResolveWhenThereIsNoTrailingSlashInBase() throws Exception {
        MutableUri uri = uri("http://www.example.com");
        MutableUri resolved = uri.resolve(uri("c%3Dd"));
        // Do not use uri() here because resolution knows %3d is a path element
        // where http://...comc%3Dd is see as part of the hostname (no path element)
        assertThat(resolved.toString()).isEqualTo("http://www.example.comc%3Dd");
        assertThat(resolved.getPathElements().toString()).isEqualTo("c=d");
    }

    @Test
    public void shouldNotFailForInvalidResourcePaths() throws Exception {
        MutableUri uri = uri("http://www.example.com///");
        assertThat(uri.toString()).isEqualTo("http://www.example.com///");
        assertThat(uri.getPathElements()).containsExactly("", "", "");
    }
}
