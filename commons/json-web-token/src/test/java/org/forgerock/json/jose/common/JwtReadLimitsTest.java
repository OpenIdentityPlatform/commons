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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.json.jose.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonException;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JweCompressionException;
import org.forgerock.json.jose.jwe.handlers.compression.DeflateCompressionHandler;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jwk.OctJWK;
import org.forgerock.json.jose.utils.Utils;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

public class JwtReadLimitsTest {

    @Test
    public void appliesLimitsConfiguredAtJvmStartup() throws Exception {
        checkJvmReadLimits("6", "8", "override");
    }

    @Test
    public void preservesStricterJacksonTokenLimit() throws Exception {
        checkJvmReadLimits("12", "8", "stricter");
    }

    @Test
    public void usesDefaultsForInvalidProperties() throws Exception {
        checkJvmReadLimits("invalid", "0", "fallback");
    }

    private void checkJvmReadLimits(String maxJsonTokens, String maxDecompressedBytes, String mode)
            throws IOException, InterruptedException {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process check = new ProcessBuilder(java.toString(),
                "-Dorg.openidentityplatform.jwt.maxJsonTokens=" + maxJsonTokens,
                "-Dorg.openidentityplatform.jwt.maxDecompressedBytes=" + maxDecompressedBytes,
                "-cp", classpath, StartupCheck.class.getName(), mode)
                .redirectErrorStream(true)
                .start();
        try {
            assertThat(check.waitFor(30, TimeUnit.SECONDS)).as("JWT limit check finished").isTrue();
            String output = new String(check.getInputStream().readAllBytes(), UTF_8);
            assertThat(check.exitValue()).as("JWT limit check output%n%s", output).isZero();
        } finally {
            if (check.isAlive()) {
                check.destroyForcibly();
            }
        }
    }

    public static class StartupCheck {

        private static final int DEFAULT_MAX_JSON_TOKENS = 65_536;
        private static final int DEFAULT_MAX_DECOMPRESSED_BYTES = 1_048_576;

        public static void main(String[] args) {
            String mode = args[0];
            if ("stricter".equals(mode)) {
                StreamReadConstraints.overrideDefaultStreamReadConstraints(
                        StreamReadConstraints.builder().maxTokenCount(6).build());
            }

            String twoRoles = "{\"roles\":[\"member\",\"editor\"]}";
            assertThat(Utils.parseJson("{\"roles\":[\"member\"]}")).containsKey("roles");

            DeflateCompressionHandler deflate = new DeflateCompressionHandler();
            byte[] atLimit = new byte[8];
            byte[] overLimit = deflate.compress(new byte[9]);
            assertThat(deflate.decompress(deflate.compress(atLimit))).isEqualTo(atLimit);
            String jwkWithId = "{\"kty\":\"oct\",\"k\":\"YQ\",\"kid\":\"old\",\"kid\":\"current\"}";
            String jwkSetWithMetadata = "{\"keys\":[],\"issuer\":\"example\"}";
            assertThat(JWK.parse("{\"kty\":\"oct\",\"k\":\"YQ\"}")).isInstanceOf(OctJWK.class);
            assertThat(JWKSet.parse("{\"keys\":[]}").getJWKsAsList()).isEmpty();

            if ("fallback".equals(mode)) {
                assertThat(JWK.parse(jwkWithId).getKeyId()).isEqualTo("current");
                assertThat(JWKSet.parse(jwkSetWithMetadata).getJWKsAsList()).isEmpty();
                assertThat(Utils.parseJson(twoRoles)).containsKey("roles");
                assertThat(deflate.decompress(overLimit)).hasSize(9);
                assertRejectsOverDefaultLimits(deflate);
            } else {
                assertRejectsOverJsonTokenLimit(twoRoles, jwkWithId, jwkSetWithMetadata);
                assertThatThrownBy(() -> deflate.decompress(overLimit)).isInstanceOf(JweCompressionException.class);
            }
        }

        private static void assertRejectsOverDefaultLimits(DeflateCompressionHandler deflate) {
            // The document contributes twelve tokens in addition to the metadata values.
            String metadata = "0,".repeat(DEFAULT_MAX_JSON_TOKENS - 12) + "0";
            String json = "{\"kty\":\"oct\",\"k\":\"YQ\",\"keys\":[],\"metadata\":[" + metadata + "]}";
            assertRejectsOverJsonTokenLimit(json, json, json);

            byte[] payload = new byte[DEFAULT_MAX_DECOMPRESSED_BYTES + 1];
            new Random(0).nextBytes(payload);
            byte[] compressed = deflate.compress(payload);
            assertThatThrownBy(() -> deflate.decompress(compressed)).isInstanceOf(JweCompressionException.class);
        }

        private static void assertRejectsOverJsonTokenLimit(String jwtJson, String jwkJson, String jwksJson) {
            assertThatThrownBy(() -> Utils.parseJson(jwtJson))
                    .isInstanceOf(InvalidJwtException.class)
                    .hasCauseInstanceOf(StreamConstraintsException.class);
            assertThatThrownBy(() -> JWK.parse(jwkJson))
                    .isInstanceOf(JsonException.class)
                    .hasCauseInstanceOf(StreamConstraintsException.class);
            assertThatThrownBy(() -> JWKSet.parse(jwksJson))
                    .isInstanceOf(JsonException.class)
                    .hasCauseInstanceOf(StreamConstraintsException.class);
        }
    }
}
