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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.http.serialization;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
public class JacksonJsonSerialization {

    private static final JsonValue TARGET = json(object(
            field("string", "a string"),
            field("number", 145.644D),
            field("bool", true),
            field("obj", object(
                    field("localizable", new LocalizableString("not localizable"))
            )),
            field("array", json(array(
                    "1",
                    "2",
                    "3"
            )))
    ));

    private static final PreferredLocales LOCALES = new PreferredLocales();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModules(new Json.JsonValueModule(), new Json.LocalizableStringModule());

    @Benchmark
    public byte[] testDefaultJson() throws IOException {
        return Json.writeJson(TARGET);
    }

    @Benchmark
    public byte[] testConstructObjectMapperEachTime() throws IOException {
        return new ObjectMapper()
                .registerModules(new Json.JsonValueModule(), new Json.LocalizableStringModule())
                .writeValueAsBytes(TARGET);
    }

    @Benchmark
    public byte[] testWithAttribute() throws IOException {
        return MAPPER.writer()
                .withAttribute(Json.PREFERRED_LOCALES_ATTRIBUTE, LOCALES)
                .writeValueAsBytes(TARGET);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JacksonJsonSerialization.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
