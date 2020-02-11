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
 * Copyright 2010–2011 ApexIdentity Inc.
 * Portions Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.http.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.forgerock.http.protocol.Form;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class UrisTest {

    @Test
    public void toURIandBack() throws Exception {
        URI u1 = Uris.create("a", "b", "c", 4, "/e%3D", "x=%3D&nullvalue", "g%3D");
        URI u2 = Uris.create(u1.getScheme(), u1.getRawUserInfo(), u1.getHost(),
                             u1.getPort(), u1.getRawPath(), u1.getRawQuery(), u1.getRawFragment());
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    public void rawParams() throws Exception {
        URI uri = Uris.create("http", "user", "example.com", 80, "/raw%3Dpath",
                              "x=%3D", "frag%3Dment");
        assertThat(uri.toString()).isEqualTo("http://user@example.com:80/raw%3Dpath?x=%3D#frag%3Dment");
    }

    @Test
    public void rebase() throws Exception {
        URI uri = new URI("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        URI base = new URI("http://www.example.com/");
        URI rebased = Uris.rebase(uri, base);
        assertThat(rebased.toString()).isEqualTo("http://www.example.com/all/good/things?come=to&those=who#breakdance");
    }

    @Test
    public void testWithQuery() throws Exception {
        URI uri = new URI("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        Form form = new Form();
        form.add("goto", "http://some.url");
        form.add("state", "1234567890");
        form.add("nullvalue", null);
        URI withQuery = Uris.withQuery(uri, form);
        // Form uses LinkedHashMap so parameter order is guaranteed.
        assertThat(withQuery.toString()).isEqualTo(
                "https://doot.doot.doo.org/all/good/things?goto=http://some.url&state=1234567890&nullvalue#breakdance");
    }

    @Test
    public void testWithoutQueryAndFragment() throws Exception {
        URI uri = new URI("https://doot.doot.doo.org/all/good/things?come=to&those=who#breakdance");
        URI withoutQueryAndFragment = Uris.withoutQueryAndFragment(uri);
        assertThat(withoutQueryAndFragment.toString()).isEqualTo(
                "https://doot.doot.doo.org/all/good/things");
    }

    @DataProvider
    private Object[][] illegalQueryStrings() {
        return new Object[][] {
            { "param1=white space&param2=value2", "param1=white%20space&param2=value2" },
            { "param1=\"double quotes\"&param2=value2", "param1=%22double%20quotes%22&param2=value2" },
            { "param1=white%20space&param2=\"double quotes\"", "param1=white%20space&param2=%22double%20quotes%22"}
        };
    }

    @Test(expectedExceptions = URISyntaxException.class, dataProvider = "illegalQueryStrings")
    public void testCreateRejectsIllegalQueryStrings(String unsafeQuery, String safeQuery) throws Exception {
        Uris.create("http", null, "localhost", 8080, "raw/path", unsafeQuery, null);
    }

    @Test(dataProvider = "illegalQueryStrings")
    public void testCreateNonStrictAcceptsIllegalQueryStrings(String unsafeQuery, String safeQuery) throws Exception {
        URI uri = Uris.createNonStrict("http", null, "localhost", 8080, "raw/path", unsafeQuery, null);
        assertThat(uri.getRawQuery()).isEqualTo(safeQuery);
    }

    @DataProvider
    private Object[][] urlEncodings() {
        //    decoded,       path encoded,  query encoded, form encoded
        return new Object[][] {
            { null,          null,          null,                null },                              // empty
            { "",            "",            "",                  "" },                                // empty
            { " ",           "%20",         "%20",               "+" },                               // whitespace
            { "azAZ09-._~",  "azAZ09-._~",  "azAZ09-._~",        "azAZ09-._%7E" },                    // unreserved
            { "!$&'()*+,;=", "!$&'()*+,;=", "!$%26'()*%2B,;%3D", "%21%24%26%27%28%29*%2B%2C%3B%3D" }, // sub-delims
            { ":@",          ":@",          ":@",                "%3A%40" },                          // pchar
            { "/?",          "%2F%3F",      "/?",                "%2F%3F" },                          // query
        };
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlPathEncode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.urlEncodePathElement(decoded)).isEqualTo(pathEncoded);
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlQueryEncode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.urlEncodeQueryParameterNameOrValue(decoded)).isEqualTo(queryEncoded);
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlFormEncode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.formEncodeParameterNameOrValue(decoded)).isEqualTo(formEncoded);
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlPathDecode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.urlDecodePathElement(pathEncoded)).isEqualTo(decoded);
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlQueryDecode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.urlDecodeQueryParameterNameOrValue(queryEncoded)).isEqualTo(decoded);
    }

    @Test(dataProvider = "urlEncodings")
    public void testUrlFormDecode(String decoded, String pathEncoded, String queryEncoded, String formEncoded) {
        assertThat(Uris.formDecodeParameterNameOrValue(formEncoded)).isEqualTo(decoded);
    }

    @Test
    public void testUrlQueryDecodeConvertsPlusToSpace() {
        assertThat(Uris.urlDecodeQueryParameterNameOrValue("%20%2B+")).isEqualTo(" + ");
    }
}
