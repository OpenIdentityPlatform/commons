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

package org.forgerock.http.header;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.forgerock.http.protocol.Header;
import org.testng.annotations.Test;



@SuppressWarnings("javadoc")
public class AbstractSingleValuedHeaderFactoryTest {

    static class SingleValueHeader extends Header {

        private final String value;

        public SingleValueHeader(String value) {
            this.value = value;
        }

        @Override
        public String getName() {
            return "TEST";
        }

        @Override
        public List<String> getValues() {
            return Collections.singletonList(value);
        }

    }

    static class Factory extends AbstractSingleValuedHeaderFactory<SingleValueHeader> {

        @Override
        protected SingleValueHeader parse(String value) throws MalformedHeaderException {
            return new SingleValueHeader(value);
        }

    }

    @Test
    public void shouldCreateHeader() throws Exception {
        assertThat(new Factory().parse(Collections.singletonList("foo"))).isNotNull();
    }

    @Test
    public void shouldReturnNullWhenNoValue() throws Exception {
        assertThat(new Factory().parse(Collections.emptyList())).isNull();
    }

    @Test(expectedExceptions = MalformedHeaderException.class)
    public void shouldFailWhenMoreThanOneValue() throws Exception {
        new Factory().parse(Arrays.asList("foo", "bar"));
    }


}
