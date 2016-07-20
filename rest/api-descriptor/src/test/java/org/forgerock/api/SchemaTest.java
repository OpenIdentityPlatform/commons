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
package org.forgerock.api;

import org.forgerock.api.markdown.MarkdownReader;
import org.forgerock.api.markdown.PropertyRecord;
import org.forgerock.api.markdown.TypeDescriptor;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.http.routing.Version;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the beans and builders against the markdown document.
 */
public class SchemaTest {

    public static final String PUT = "put";
    public static final String ENUMS_PACKAGE = "org.forgerock.api.enums";
    public static final String ARRAY_CLASS_NAME_FORMAT = "[L%s";
    private MarkdownReader mdReader;
    private List<TypeDescriptor> typeDescriptors;
    private static final String NO_SUCH_METHOD_EXCEPTION_MESSAGE = "# %s(%s) method not available in %s type.";
    private static final String MAP_PARAMETER_TYPE_MESSAGE = "java.util.Map<java.lang.String,%s>";
    private static final Map<String, Class<?>> MAP_KEY_TYPES = new HashMap<String, Class<?>>() { {
            put("VersionedPath", Version.class);
        } };

    @BeforeTest
    public void before() {
        mdReader = new MarkdownReader();
        typeDescriptors = mdReader.generateTypeDescriptorList();
    }

    @Test
    public void schemaObjectCheckAgainstMarkdownDocumentTest() throws ClassNotFoundException, NoSuchMethodException {
        for (TypeDescriptor typeDescriptor : typeDescriptors) {
            //We are not checking the Schema bean at the moment
            if (!typeDescriptor.getName().equals("Schema")) {
                checkApiBean(typeDescriptor);
            }
        }
    }

    public void checkApiBean(TypeDescriptor typeDescriptor) throws ClassNotFoundException, NoSuchMethodException {
        Class builderClass = Class.forName(
                MarkdownReader.API_DESCRIPTION_BEANS_PACKAGE + typeDescriptor.getName() + "$Builder");
        Method[] methods = builderClass.getMethods();
        checkSuperclass(typeDescriptor);
        assertThat(checkMethodNamesAndParameters(methods, typeDescriptor.getProperties(), typeDescriptor.getName()));
    }

    private void checkSuperclass(TypeDescriptor typeDescriptor) throws ClassNotFoundException {
        if (typeDescriptor.getSuperClass() != null) {
            String superClass = Class.forName(MarkdownReader.API_DESCRIPTION_BEANS_PACKAGE + typeDescriptor.getName())
                    .getSuperclass().getName();
            if (!superClass.equals(typeDescriptor.getSuperClass())) {
                throw new java.lang.ClassNotFoundException(
                        typeDescriptor.getName() + " does not extend " + typeDescriptor.getSuperClass());
            }
        }
    }

    private boolean checkMethodNamesAndParameters(Method[] methods, List<PropertyRecord> properties, String typeName)
            throws NoSuchMethodException, ClassNotFoundException {
        for (PropertyRecord property : properties) {
            String methodName = isMapType(property) ? PUT : property.getKey();
            Method method = findMethod(methods, methodName);
            checkIfMethodAvailable(property, method, typeName);
        }
        return true;
    }

    private boolean isMapType(PropertyRecord property) {
        return property.getKey().contains("*") || property.getKey().contains("[");
    }

    private void checkIfMethodAvailable(PropertyRecord property, Method method, String typeName)
            throws NoSuchMethodException, ClassNotFoundException {
        if (method == null) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, property.getKey(), property.getType(), typeName));
        } else if (!method.getName().equalsIgnoreCase(PUT)
                && !isValidSimpleType(method, property.getType(), property.isEnumType())) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, property.getKey(), property.getType(), typeName));
        } else if (method.getName().equalsIgnoreCase(PUT)
                && !isValidMapType(method, property.getType())) {
            throw new NoSuchMethodException(
                    String.format(NO_SUCH_METHOD_EXCEPTION_MESSAGE, PUT,
                            String.format(MAP_PARAMETER_TYPE_MESSAGE, property.getType(), typeName)));
        }
    }

    private boolean isValidSimpleType(Method method, String type, boolean isEnumType) throws ClassNotFoundException {
        Class<?> methodParameter = method.getParameterTypes()[0];
        String methodParamType = methodParameter.getName();
        String methodParamPckgName = (methodParameter.getPackage() != null)
                ? methodParameter.getPackage().getName() : "";

        if (type.endsWith("[]")) {
            return methodParamType.equals(List.class.getName())
                    || (!isEnumType && methodParamType.equals(toArrayClassName(type)))
                    || (isEnumType && methodParamType.startsWith(
                    String.format(ARRAY_CLASS_NAME_FORMAT, ENUMS_PACKAGE)));
        } else {
            return (!isEnumType && (methodParamType.equals(type) || isEquivalent(type, methodParamType)))
                    || (isEnumType && methodParamPckgName.startsWith(ENUMS_PACKAGE));
        }
    }

    private boolean isEquivalent(String type, String param) {
        return type.equals(String.class.getCanonicalName()) && param.equals(LocalizableString.class.getCanonicalName());
    }

    private String toArrayClassName(String type) {
        return String.format(ARRAY_CLASS_NAME_FORMAT, type.substring(0, type.indexOf("["))) + ";";
    }

    private boolean isValidMapType(Method method, String type) throws ClassNotFoundException {
        Class<?> keyType = MAP_KEY_TYPES.get(method.getDeclaringClass().getEnclosingClass().getSimpleName());
        return method.getParameterTypes()[0].getName().equals((keyType == null ? String.class : keyType).getName())
                && (method.getParameterTypes()[1].getName().equals(type)
                || method.getParameterTypes()[1].getName().equals(
                Class.forName(type).getInterfaces()[0].getName()));
    }

    private Method findMethod(Method[] methods, String key) {
        Method method = null;
        for (Method m : methods) {
            if (m.getName().equals(key)) {
                if (method == null) {
                    method = m;
                } else {
                    method = filterOutJsonAnySetterMethod(method, m);
                }
            }
        }
        return method;
    }

    private Method filterOutJsonAnySetterMethod(Method storedMethod, Method newMethod) {
        return storedMethod.getAnnotations() == null || storedMethod.getAnnotations().length == 0
                ? storedMethod
                : newMethod;
    }
}