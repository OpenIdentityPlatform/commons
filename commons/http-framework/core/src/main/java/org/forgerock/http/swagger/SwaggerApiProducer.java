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
package org.forgerock.http.swagger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.forgerock.http.util.Paths.addLeadingSlash;
import static org.forgerock.http.util.Paths.removeTrailingSlash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.routing.Version;

import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;

/**
 * An API Producer for APIs that use the Swagger model implementation of the OpenAPI specification.
 */
public class SwaggerApiProducer implements ApiProducer<Swagger> {

    private final List<Scheme> schemes;
    private final String basePath;
    private final Info info;
    private final String host;

    /**
     * Create a new API Description Producer with {@literal null} as basePath, host and no scheme.
     *
     * @param info The Swagger {@code Info} instance to add to all OpenAPI descriptors.
     */
    public SwaggerApiProducer(Info info) {
        this(info, null, null, Collections.<Scheme> emptyList());
    }

    /**
     * Create a new API Description Producer.
     *
     * @param info The Swagger {@code Info} instance to add to all OpenAPI descriptors.
     * @param basePath The base path.
     * @param host The host, if known at construction time, otherwise null.
     * @param schemes The supported schemes.
     */
    public SwaggerApiProducer(Info info, String basePath, String host, Scheme... schemes) {
        this(info, basePath, host, asList(schemes));
    }

    /**
     * Create a new API Description Producer.
     *
     * @param info The Swagger {@code Info} instance to add to all OpenAPI descriptors.
     * @param basePath The base path.
     * @param host The host, if known at construction time, otherwise null.
     * @param schemes The supported schemes.
     */
    public SwaggerApiProducer(Info info, String basePath, String host, List<Scheme> schemes) {
        this.info = info;
        this.basePath = basePath;
        this.host = host;
        this.schemes = new ArrayList<>(schemes);
    }

    @Override
    public Swagger withPath(Swagger descriptor, String parentPath) {
        return transform(descriptor, new PathTransformer(parentPath));
    }

    private static class PathTransformer implements Function<Map<String, Path>, Map<String, Path>> {

        private final String parentPath;

        PathTransformer(String parentPath) {
            this.parentPath = addLeadingSlash(removeTrailingSlash(parentPath));
        }

        @Override
        public Map<String, Path> apply(Map<String, Path> pathMap) {
            Map<String, Path> result = new HashMap<>(pathMap.size());
            for (Map.Entry<String, Path> entry : pathMap.entrySet()) {
                String key = entry.getKey();
                result.put(parentPath + addLeadingSlash(key), entry.getValue());
            }
            return result;
        }

    }

    @Override
    public Swagger withVersion(Swagger descriptor, Version version) {
        return transform(descriptor, new VersionTransformer(version));
    }

    private static class VersionTransformer implements Function<Map<String, Path>, Map<String, Path>> {

        public static final String PATH_FRAGMENT_MARKER = "#";
        public static final String PATH_FRAGMENT_COMPONENT_SEPARATOR = "_";
        private final Version version;

        VersionTransformer(Version version) {
            this.version = version;
        }

        @Override
        public Map<String, Path> apply(Map<String, Path> pathMap) {
            Map<String, Path> result = new HashMap<>(pathMap.size());
            for (Map.Entry<String, Path> entry : pathMap.entrySet()) {
                String key = entry.getKey();
                Path path = entry.getValue();
                HeaderParameter acceptVersionHeader = new HeaderParameter()
                        .name(AcceptApiVersionHeader.NAME)
                        ._enum(singletonList(AcceptApiVersionHeader.RESOURCE + "=" + version));
                path.addParameter(acceptVersionHeader);
                if (key.contains(PATH_FRAGMENT_MARKER)) {
                    result.put(key + PATH_FRAGMENT_COMPONENT_SEPARATOR + version, path);
                } else {
                    result.put(key + PATH_FRAGMENT_MARKER + version, path);
                }
            }
            return result;
        }

    }

    private Swagger transform(Swagger descriptor, Function<Map<String, Path>,
            Map<String, Path>> transformer) {
        Swagger swagger = addApiInfo(SwaggerUtils.clone(descriptor));
        swagger.setPaths(transformer.apply(descriptor.getPaths()));
        return swagger;
    }

    @Override
    public Swagger merge(List<Swagger> descriptors) {
        descriptors = new ArrayList<>(descriptors);
        descriptors.removeAll(Collections.<Swagger>singletonList(null));
        if (descriptors.isEmpty()) {
            return null;
        }

        Swagger swagger = addApiInfo(new SwaggerExtended());
        for (Swagger descriptor : descriptors) {
            for (String consumes : ensureNotNull(descriptor.getConsumes())) {
                swagger.consumes(consumes);
            }
            for (String produces : ensureNotNull(descriptor.getProduces())) {
                swagger.produces(produces);
            }
            for (Tag tag : ensureNotNull(descriptor.getTags())) {
                swagger.addTag(tag);
            }
            for (Map.Entry<String, Response> response : ensureNotNull(descriptor.getResponses()).entrySet()) {
                if (isUndefinedEntry("response", response, swagger.getResponses())) {
                    swagger.response(response.getKey(), response.getValue());
                }
            }
            for (Map.Entry<String, Parameter> parameter : ensureNotNull(descriptor.getParameters()).entrySet()) {
                if (isUndefinedEntry("parameter", parameter, swagger.getParameters())) {
                    swagger.addParameter(parameter.getKey(), parameter.getValue());
                }
            }
            for (Map.Entry<String, Object> extension : ensureNotNull(descriptor.getVendorExtensions()).entrySet()) {
                if (isUndefinedEntry("extension", extension, swagger.getVendorExtensions())) {
                    swagger.vendorExtension(extension.getKey(), extension.getValue());
                }
            }
            for (Map.Entry<String, Model> definition : ensureNotNull(descriptor.getDefinitions()).entrySet()) {
                if (isUndefinedEntry("definition", definition, swagger.getDefinitions())) {
                    swagger.addDefinition(definition.getKey(), definition.getValue());
                }
            }
            for (Map.Entry<String, Path> path : ensureNotNull(descriptor.getPaths()).entrySet()) {
                validatePathNotDefined(path.getKey(), ensureNotNull(swagger.getPaths()).keySet());
                swagger.path(path.getKey(), path.getValue());
            }
            for (SecurityRequirement security : ensureNotNull(descriptor.getSecurity())) {
                swagger.security(security);
            }
            Map<String, SecuritySchemeDefinition> schemeDefinitionMap = ensureNotNull(descriptor
                    .getSecurityDefinitions());
            for (Map.Entry<String, SecuritySchemeDefinition> secDef : schemeDefinitionMap.entrySet()) {
                if (isUndefinedEntry("security definition", secDef, swagger.getSecurityDefinitions())) {
                    swagger.securityDefinition(secDef.getKey(), secDef.getValue());
                }
            }
        }
        return swagger;
    }

    private <T> Map<String, T> ensureNotNull(Map<String, T> map) {
        return map == null ? Collections.<String, T>emptyMap() : map;
    }

    private <T> List<T> ensureNotNull(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    @Override
    public Swagger addApiInfo(Swagger swagger) {
        if (info != null) {
            swagger.info(info.mergeWith(swagger.getInfo()));
        }
        return swagger.host(host).basePath(basePath).schemes(schemes);
    }

    private <V> boolean isUndefinedEntry(String entryType, Map.Entry<String, V> entry, Map<String, V> existing) {
        V value = existing == null ? null : existing.get(entry.getKey());
        if (value == null) {
            return true;
        }
        if (value.equals(entry.getValue())) {
            return false;
        }
        throw new IllegalArgumentException("Duplicated key for " + entryType + " but different value. Already got "
                + value);
    }

    private void validatePathNotDefined(String path, Set<String> paths) {
        if (paths.contains(path)) {
            throw new IllegalArgumentException("Duplicated path");
        }
    }

    @Override
    public ApiProducer<Swagger> newChildProducer(String subPath) {
        return new SwaggerApiProducer(info, isNullOrEmpty(basePath) ? subPath : basePath + subPath, host, schemes);
    }
}
