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

package org.forgerock.api.jackson;

import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.api.jackson.JacksonUtils.OBJECT_MAPPER;
import static org.forgerock.api.jackson.JacksonUtils.schemaFor;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.util.Reject.checkNotNull;
import static org.forgerock.util.test.assertj.Conditions.equalTo;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.assertj.core.data.MapEntry;
import org.forgerock.api.annotations.AdditionalProperties;
import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.EnumTitle;
import org.forgerock.api.annotations.Example;
import org.forgerock.api.annotations.Format;
import org.forgerock.api.annotations.MultipleOf;
import org.forgerock.api.annotations.PropertyOrder;
import org.forgerock.api.annotations.PropertyPolicies;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;
import org.forgerock.api.annotations.UniqueItems;
import org.forgerock.api.enums.ReadPolicy;
import org.forgerock.api.enums.WritePolicy;
import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

public class JsonSchemaExtensionsTest {

    private JsonValue hasIntegerWithMinMaxSchema;
    private JsonValue hasIntegerWithDecimalMinMaxSchema;
    private JsonValue hasStringSchema;
    private JsonValue hasArraySchema;
    private JsonValue hasNumericSchema;
    private JsonValue hasDate;
    private JsonValue hasAdditionalProperties;

    @BeforeClass
    public void beforeClass() throws IOException {
        hasIntegerWithMinMaxSchema = schemaAsJsonValue(HasIntegerWithMinMax.class);
        hasIntegerWithDecimalMinMaxSchema = schemaAsJsonValue(HasIntegerWithDecimalMinMaxExclusive.class);
        hasStringSchema = schemaAsJsonValue(HasString.class);
        hasArraySchema = schemaAsJsonValue(HasArray.class);
        hasNumericSchema = schemaAsJsonValue(HasNumeric.class);
        hasDate = schemaAsJsonValue(HasDate.class);
        hasAdditionalProperties = schemaAsJsonValue(HasAdditionalProperties.class);
    }

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][]{
                {hasIntegerWithMinMaxSchema, "myInt", entry("type", "integer")},
                {hasIntegerWithMinMaxSchema, "myInt", entry("minimum", 1)},
                {hasIntegerWithMinMaxSchema, "myInt", entry("maximum", 10)},
                {hasIntegerWithMinMaxSchema, "myInt", entry("multipleOf", 1.0)},

                {hasIntegerWithDecimalMinMaxSchema, "myInt", entry("minimum", 1)},
                {hasIntegerWithDecimalMinMaxSchema, "myInt", entry("maximum", 10)},
                {hasIntegerWithDecimalMinMaxSchema, "myInt", entry("exclusiveMinimum", true)},
                {hasIntegerWithDecimalMinMaxSchema, "myInt", entry("exclusiveMaximum", true)},
                {hasIntegerWithDecimalMinMaxSchema, "myInt", entry("readOnly", true)},

                {hasStringSchema, "myString", entry("default", "myDefault")},
                {hasStringSchema, "myString", entry("pattern", "[a-zA-Z]{1,100}")},
                {hasStringSchema, "myString", entry("minLength", 1)},
                {hasStringSchema, "myString", entry("maxLength", 100)},
                {hasStringSchema, "myString", entry("title", "HasString Title")},
                {hasStringSchema, "myString", entry("description", "HasString Description")},
                {hasStringSchema, "myString", entry("propertyOrder", 1)},
                {hasStringSchema, "myString", entry("writePolicy", WritePolicy.WRITE_ONCE.name())},
                {hasStringSchema, "myString", entry("readPolicy", ReadPolicy.USER.name())},
                {hasStringSchema, "myString", entry("errorOnWritePolicyFailure", true)},
                {hasStringSchema, "myString", entry("returnOnDemand", true)},
                {hasStringSchema, "hostname", entry("format", "hostname")},

                {hasArraySchema, "myArray", entry("minItems", 1)},
                {hasArraySchema, "myArray", entry("maxItems", 100)},
                {hasArraySchema, "myArray", entry("uniqueItems", true)},

                {hasNumericSchema, "myInt", entry("format", "int32")},
                {hasNumericSchema, "myInt", entry("minimum", Integer.MIN_VALUE)},
                {hasNumericSchema, "myInt", entry("maximum", Integer.MAX_VALUE)},
                {hasNumericSchema, "myLong", entry("format", "int64")},
                {hasNumericSchema, "myLong", entry("minimum", Long.MIN_VALUE)},
                {hasNumericSchema, "myLong", entry("maximum", Long.MAX_VALUE)},
                {hasNumericSchema, "myDouble", entry("format", "double")},
                {hasNumericSchema, "myDouble", entry("minimum", Double.MIN_VALUE)},
                {hasNumericSchema, "myDouble", entry("maximum", Double.MAX_VALUE)},
                {hasNumericSchema, "myFloat", entry("format", "float")},

                // NOTE: SerializationFeature.WRITE_DATES_AS_TIMESTAMPS must be disabled on Jackson ObjectMapper
                {hasDate, "myDate", entry("type", "string")},
                {hasDate, "myDate", entry("format", "date-time")}
        };
    }

    @Test(dataProvider = "data")
    public void test(final JsonValue schema, final String fieldName, final MapEntry entry) {
        assertThat(schema)
                .hasObject("properties")
                .hasObject(fieldName)
                .contains(entry);
    }

    @Test
    public void testAdditionalProperties() {
        assertThat(hasAdditionalProperties)
                .hasObject("additionalProperties")
                .contains(entry("type", "string"));
    }

    @Test
    public void testEnumSchema() throws IOException {
        final JsonValue schema = schemaAsJsonValue(HasEnum.class);

        // enum
        assertThat(schema)
                .hasObject("properties")
                .hasObject("myEnum")
                .contains("type", "string")
                .hasArray("enum")
                .containsExactly("ONE", "TWO", "THREE");

        // options/enum_titles
        assertThat(schema)
                .hasObject("properties")
                .hasObject("myEnum")
                .contains("type", "string")
                .hasObject("options")
                .hasArray("enum_titles")
                .containsExactly("One", "Two", "Three");

        // enumTitles
        assertThat(schema)
                .hasObject("properties")
                .hasObject("myEnum")
                .contains("type", "string")
                .hasArray("enumTitles")
                .containsExactly("One", "Two", "Three");

        // type's required fields
        assertThat(schema)
                .hasArray("required")
                .containsExactly("myEnum");

        // type's title
        assertThat(schema)
                .hasPath("title")
                .isString()
                .isEqualTo("HasEnum Title");

        // type's description
        assertThat(schema)
                .hasPath("description")
                .isString()
                .isEqualTo("HasEnum Description");
    }

    @Test
    public void testExamples() throws Exception {
        final JsonValue schema = schemaAsJsonValue(HasExamples.class);

        assertThat(schema)
                .hasPath("properties/array/example")
                .isArray()
                .containsOnly("ONE", "TWO");

        assertThat(schema)
                .hasPath("properties/obj/example")
                .isObject()
                .containsExactly(entry("name", "fred"));

        assertThat(schema).hasPath("properties/string/example").isString().isEqualTo("fred");
        assertThat(schema).hasPath("properties/integer/example").isLong().isEqualTo(123456789012345678L);
        assertThat(schema).hasPath("properties/number/example").isDouble().isEqualTo(123.456D);
        assertThat(schema).hasPath("properties/bool/example").isBoolean().isTrue();

        assertThat(schema)
                .hasPath("example")
                .isObject()
                .stringIs("string", equalTo("fred"))
                .longIs("integer", equalTo(123456789012345678L))
                .doubleIs("number", equalTo(123.456D))
                .booleanIs("bool", equalTo(true));
        assertThat(schema).hasPath("example/array").isArray().hasSize(2);
        assertThat(schema).hasPath("example/obj").isObject().containsFields("name");
    }

    @Test
    public void testClassExample() throws Exception {
        final JsonValue schema = schemaAsJsonValue(ClassHasExample.class);

        assertThat(schema)
                .hasPath("example")
                .isObject()
                .stringIs("string", equalTo("fred"))
                .longIs("integer", equalTo(123456789012345678L));
        assertThat(schema).hasPath("properties/integer/example").isLong().isEqualTo(1234567890123456L);
    }

    private enum MyEnum {
        @EnumTitle("One")
        ONE,
        @EnumTitle("Two")
        TWO,
        @EnumTitle("Three")
        THREE
    }

    @Title("HasEnum Title")
    @Description("HasEnum Description")
    private static class HasEnum {
        @NotNull
        private MyEnum myEnum;

        public MyEnum getMyEnum() {
            return myEnum;
        }

        public void setMyEnum(MyEnum myEnum) {
            this.myEnum = myEnum;
        }
    }

    private static class HasIntegerWithMinMax {
        @Min(1)
        @Max(10)
        @MultipleOf(1)
        Integer myInt;

        public Integer getMyInt() {
            return myInt;
        }
    }

    private static class HasIntegerWithDecimalMinMaxExclusive {
        @DecimalMin(value = "1", inclusive = false)
        @DecimalMax(value = "10", inclusive = false)
        @ReadOnly
        private int myInt;

        public int getMyInt() {
            return myInt;
        }
    }

    private static class HasString {
        @PropertyPolicies(
                write = WritePolicy.WRITE_ONCE,
                read = ReadPolicy.USER,
                errorOnWritePolicyFailure = true,
                returnOnDemand = true)
        @PropertyOrder(1)
        @Size(min = 1, max = 100)
        @Pattern(regexp = "[a-zA-Z]{1,100}")
        @Default("myDefault")
        @Title("HasString Title")
        @Description("HasString Description")
        String myString = "myDefault";

        @Format("hostname")
        String hostname;

        public String getMyString() {
            return myString;
        }

        public String getHostname() {
            return hostname;
        }
    }

    private static class HasArray {
        @Size(min = 1, max = 100)
        @UniqueItems
        String[] myArray;

        public String[] getMyArray() {
            return myArray;
        }
    }

    private static class HasNumeric {
        @Min(Integer.MIN_VALUE)
        @Max(Integer.MAX_VALUE)
        @Format("int32")
        int myInt;

        @Min(Long.MIN_VALUE)
        @Max(Long.MAX_VALUE)
        @Format("int64")
        long myLong;

        @DecimalMin(value = "4.9E-324")
        @DecimalMax(value = "1.7976931348623157E308")
        @Format("double")
        double myDouble;

        // NOTE: Java cannot accurately compare MIN/MAX Float values, so we rely on DecimalMin/DecimalMax above to test
        @Format("float")
        float myFloat;

        public int getMyInt() {
            return myInt;
        }

        public long getMyLong() {
            return myLong;
        }

        public double getMyDouble() {
            return myDouble;
        }

        public float getMyFloat() {
            return myFloat;
        }
    }

    private static class HasDate {
        Date myDate;

        public Date getMyDate() {
            return myDate;
        }
    }

    private static class HasExamples {
        @Example("fred")
        public String string;
        @Example("123456789012345678")
        public Long integer;
        @Example("123.456")
        public BigDecimal number;
        @Example("true")
        public boolean bool;
        @Example("[\"ONE\",\"TWO\"]")
        public List<MyEnum> array;
        @Example("{\"name\":\"fred\"}")
        public Map<String, String> obj;
    }

    @Example("classpath:org/forgerock/api/jackson/JsonSchemaExtensionsTest.ClassHasExample.json")
    private static class ClassHasExample {
        public String string;
        @Example("1234567890123456")
        public Long integer;
    }

    @AdditionalProperties(String.class)
    private static class HasAdditionalProperties extends HashMap<String, String> {
        // empty
    }

    /**
     * Creates a JSON Schema given a Java class type.
     *
     * @param type Type to create JSON Schema for
     * @return {@link JsonValue} representation of JSON Schema
     * @throws IOException Jackson JSON serialization failure
     */
    private static JsonValue schemaAsJsonValue(final Class<?> type) throws IOException {
        final JsonSchema jsonSchema = schemaFor(checkNotNull(type, "type required"));
        final String schemaString = OBJECT_MAPPER.writer().writeValueAsString(jsonSchema);
        return json(OBJECT_MAPPER.readValue(schemaString, Object.class));
    }
}
