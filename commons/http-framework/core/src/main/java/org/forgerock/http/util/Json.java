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

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS;
import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Request;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;


/**
 * Provides read and write JSON capabilities.
 * Can check if an object reference is JSON-compatible (expressed as primitive values, list/array and map).
 */
public final class Json {

    /** Non strict object mapper / data binder used to read json configuration files/data. */
    private static final ObjectMapper LENIENT_MAPPER;
    static {
        LENIENT_MAPPER = new ObjectMapper().registerModules(new JsonValueModule(), new LocalizableStringModule());
        LENIENT_MAPPER.configure(ALLOW_COMMENTS, true);
        LENIENT_MAPPER.configure(ALLOW_SINGLE_QUOTES, true);
        LENIENT_MAPPER.configure(ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    /** Strict object mapper / data binder used to read json configuration files/data. */
    private static final ObjectMapper STRICT_MAPPER = new ObjectMapper()
            .registerModules(new JsonValueModule(), new LocalizableStringModule());

    /**
     * Attribute Key for the {@link org.forgerock.util.i18n.PreferredLocales} instance.
     */
    private static final String PREFERRED_LOCALES_ATTRIBUTE = "PreferredLocales";

    /**
     * Jackson Module that adds a serializer for {@link LocalizableString}.
     */
    public static class LocalizableStringModule extends SimpleModule {

        private static final PreferredLocales DEFAULT_PREFERRED_LOCALES = new PreferredLocales();

        /** Default constructor. */
        public LocalizableStringModule() {
            addSerializer(LocalizableString.class, new JsonSerializer<LocalizableString>() {
                @Override
                public void serialize(LocalizableString localizableString, JsonGenerator jsonGenerator,
                        SerializerProvider serializerProvider) throws IOException {
                    PreferredLocales locales =
                            (PreferredLocales) serializerProvider.getAttribute(PREFERRED_LOCALES_ATTRIBUTE);
                    if (locales == null) {
                        locales = DEFAULT_PREFERRED_LOCALES;
                    }
                    jsonGenerator.writeString(localizableString.toTranslatedString(locales));
                }
            });
        }
    }

    /**
     * Jackson Module that uses a mixin to make sure that a {@link org.forgerock.json.JsonValue} instance is
     * serialized using its {@code #getObject()} value only.
      */
    public static class JsonValueModule extends SimpleModule {
        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(org.forgerock.json.JsonValue.class, JsonValueMixin.class);
        }
    }

    private static abstract class JsonValueMixin {
        @JsonValue
        public abstract String getObject();
    }

        /**
     * Private constructor for utility class.
     */
    private Json() { }

    /**
     * Verify that the given parameter object is of a JSON compatible type (recursively). If no exception is thrown that
     * means the parameter can be used in the JWT session (that is a JSON value).
     *
     * @param trail
     *         pointer to the verified object
     * @param value
     *         object to verify
     */
    public static void checkJsonCompatibility(final String trail, final Object value) {

        // Null is OK
        if (value == null) {
            return;
        }

        Class<?> type = value.getClass();
        Object object = value;

        // JSON supports Boolean
        if (object instanceof Boolean) {
            return;
        }

        // JSON supports Chars (as String)
        if (object instanceof Character) {
            return;
        }

        // JSON supports Numbers (Long, Float, ...)
        if (object instanceof Number) {
            return;
        }

        // JSON supports String
        if (object instanceof CharSequence) {
            return;
        }

        // Consider array like a List
        if (type.isArray()) {
            object = Arrays.asList((Object[]) value);
        }

        if (object instanceof List) {
            List<?> list = (List<?>) object;
            for (int i = 0; i < list.size(); i++) {
                checkJsonCompatibility(format("%s[%d]", trail, i), list.get(i));
            }
            return;
        }

        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                checkJsonCompatibility(format("%s/%s", trail, entry.getKey()), entry.getValue());
            }
            return;
        }

        throw new IllegalArgumentException(format(
                "The object referenced through '%s' cannot be safely serialized as JSON",
                trail));
    }

    /**
     * Parses to json the provided data.
     *
     * @param rawData
     *            The data as a string to read and parse.
     * @see Json#readJson(Reader)
     * @return Any of {@code Map<String, Object>}, {@code List<Object>}, {@code Number}, {@code Boolean}
     *         or {@code null}.
     * @throws IOException
     *             If an exception occurs during parsing the data.
     */
    public static Object readJson(final String rawData) throws IOException {
        if (rawData == null) {
            return null;
        }
        return readJson(new StringReader(rawData));
    }

    /**
     * Parses to json the provided reader.
     *
     * @param reader
     *            The data to parse.
     * @return Any of {@code Map<String, Object>}, {@code List<Object>}, {@code Number}, {@code Boolean}
     *         or {@code null}.
     * @throws IOException
     *             If an exception occurs during parsing the data.
     */
    public static Object readJson(final Reader reader) throws IOException {
        return parse(STRICT_MAPPER, reader);
    }

    /**
     * This function it's only used to read our configuration files and allows
     * JSON files to contain non strict JSON such as comments or single quotes.
     *
     * @param reader
     *            The stream of data to parse.
     * @return Any of {@code Map<String, Object>}, {@code List<Object>}, {@code Number}, {@code Boolean}
     *         or {@code null}.
     * @throws IOException
     *             If an error occurs during reading/parsing the data.
     */
    public static Object readJsonLenient(final Reader reader) throws IOException {
        return parse(LENIENT_MAPPER, reader);
    }

    /**
     * This function it's only used to read our configuration files and allows
     * JSON files to contain non strict JSON such as comments or single quotes.
     *
     * @param in
     *            The input stream containing the json.
     * @return Any of {@code Map<String, Object>}, {@code List<Object>}, {@code Number}, {@code Boolean}
     *         or {@code null}.
     * @throws IOException
     *             If an error occurs during reading/parsing the data.
     */
    public static Object readJsonLenient(final InputStream in) throws IOException {
        return parse(LENIENT_MAPPER, new InputStreamReader(in));
    }

    private static Object parse(ObjectMapper mapper, Reader reader) throws IOException {
        if (reader == null) {
            return null;
        }

        return mapper.readValue(reader, Object.class);
    }

    /**
     * Writes the JSON content of the object passed in parameter.
     *
     * @param objectToWrite
     *            The object we want to serialize as JSON output. The
     * @return the Json output as a byte array.
     * @throws IOException
     *             If an error occurs during writing/mapping content.
     */
    public static byte[] writeJson(final Object objectToWrite) throws IOException {
        return STRICT_MAPPER.writeValueAsBytes(objectToWrite);
    }

    /**
     * Make an object writer that contains the locales from the request for serialization of {@link LocalizableString}
     * instances. The provided {@code mapper} will be used to create the writer so that serialization configuration is
     * not recreated.
     *
     * @param mapper The {@code ObjectMapper} to obtain a writer from.
     * @param request The CHF request.
     * @return The configured {@code ObjectWriter}.
     * @throws MalformedHeaderException If the Accept-Language header is malformed.
     */
    public static ObjectWriter makeLocalizingObjectWriter(ObjectMapper mapper, Request request)
            throws MalformedHeaderException {
        return makeLocalizingObjectWriter(mapper,
                request.getHeaders().containsKey(AcceptLanguageHeader.NAME)
                        ? request.getHeaders().get(AcceptLanguageHeader.class).getLocales()
                        : null);
    }

    /**
     * Make an object writer that contains the provided locales for serialization of {@link LocalizableString}
     * instances. The provided {@code mapper} will be used to create the writer so that serialization configuration is
     * not recreated.
     *
     * @param mapper The {@code ObjectMapper} to obtain a writer from.
     * @param locales The {@code PreferredLocales} instance to use for localization, or {@code null}.
     * @return The configured {@code ObjectWriter}.
     */
    public static ObjectWriter makeLocalizingObjectWriter(ObjectMapper mapper, PreferredLocales locales) {
        ObjectWriter writer = mapper.writer();
        if (locales != null) {
            writer = writer.withAttribute(Json.PREFERRED_LOCALES_ATTRIBUTE, locales);
        }
        return writer;
    }

}
