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

package org.forgerock.api.markdown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for the markdown file.
 */
public class MarkdownReader {

    private static final String API_DESCRIPTOR_TYPE_REGEX = "\\[.*\\]\\(\\#.*\\).*";
    private static final String FILE_NAME = "docs/spec.md";
    private static final String EXTENDS = "Extends ";
    public static final String API_DESCRIPTION_BEANS_PACKAGE = "org.forgerock.api.models.";
    public static final String SCHEMA_HEADLINE = "### Schema";
    public static final String TYPE_HEADLINE = "### ";
    public static final String TABLE_SIGN = "--------";
    public static final String SPECIFICATION_HEADLINE = "## Specification";
    public static final String SUPPORTED_VALUES_ARE = "Supported values are";
    public static final String JSON_REF_KEY = "$ref";
    public static final String JSON_REF_SCHEMA_KEY = "value";
    public static final String NO_ADDITIONAL_PROPERTIES = "No additional properties.";

    public enum ReadPointer {
        START,
        SPECIFICATION,
        TYPE,
        TABLE
    }

    private List<String> readMarkdown() {
        Path path = Paths.get(FILE_NAME);

        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public List<TypeDescriptor> generateTypeDescriptorList() {
        List<TypeDescriptor> typeDescriptors = new ArrayList<>();
        ReadPointer readPointer = ReadPointer.START;
        TypeDescriptor typeDescriptor = null;
        for (String line : readMarkdown()) {
            if (readPointer == ReadPointer.START && line.equals(SPECIFICATION_HEADLINE)) {
                readPointer = ReadPointer.SPECIFICATION;
            } else if (readPointer == ReadPointer.SPECIFICATION) {
                if (line.equals(SCHEMA_HEADLINE)) {
                    typeDescriptors.add(typeDescriptor);
                    break;
                } else if (line.startsWith(TYPE_HEADLINE)) {
                    if (typeDescriptor != null) {
                        typeDescriptors.add(typeDescriptor);
                    }
                    readPointer = ReadPointer.TYPE;
                    String typeName = line.trim().substring(TYPE_HEADLINE.length()).split(" ")[0];
                    typeDescriptor = new TypeDescriptor(typeName);
                }
            } else if (readPointer == ReadPointer.TYPE) {
                if (line.contains(EXTENDS)) {
                    typeDescriptor.setSuperClass(toFullyQualifiedClassname(line.split(EXTENDS)[1]));
                } else if (line.startsWith(TABLE_SIGN)) {
                    readPointer = ReadPointer.TABLE;
                } else if (line.startsWith(NO_ADDITIONAL_PROPERTIES)) {
                    readPointer = ReadPointer.SPECIFICATION;
                }
            } else if (readPointer == ReadPointer.TABLE) {
                if (line.trim().isEmpty()) {
                    readPointer = ReadPointer.SPECIFICATION;
                } else {
                    String[] properties = line.split("\\|");
                    String key = getKeyTransformed(properties[0]);
                    String type = toFullyQualifiedClassname(properties[1].trim());
                    boolean required = !properties[2].trim().isEmpty();
                    boolean isEnumType = properties[3].contains(SUPPORTED_VALUES_ARE);
                    PropertyRecord ppp = new PropertyRecord(key, type, required, isEnumType);
                    typeDescriptor.addProperty(ppp);
                }
            }
        }
        return typeDescriptors;
    }

    private String getKeyTransformed(String key) {
        String keyNormalized = key.replace("`", "");
        return (keyNormalized.trim().equals(JSON_REF_KEY)) ? JSON_REF_SCHEMA_KEY : keyNormalized.trim();
    }

    private static String toFullyQualifiedClassname(String className) {
        String fullyQualified = null;
        if (className.matches(API_DESCRIPTOR_TYPE_REGEX)) {
            fullyQualified = API_DESCRIPTION_BEANS_PACKAGE
                    + className.substring(className.indexOf("[") + 1, className.indexOf("]"));
        } else if (className.startsWith("Number")) {
            fullyQualified = Double.class.getName();
        } else if (className.startsWith("Integer")) {
            fullyQualified = Integer.class.getName();
        } else if (className.startsWith("String")) {
            fullyQualified = String.class.getName();
        } else if (className.startsWith("boolean")) {
            fullyQualified = Boolean.class.getName();
        } else {
            fullyQualified = className;
        }
        return (className.endsWith("[]")) ? fullyQualified + "[]" : fullyQualified;
    }

}
