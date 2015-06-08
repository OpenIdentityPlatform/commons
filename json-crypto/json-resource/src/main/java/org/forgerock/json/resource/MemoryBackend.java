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
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import static org.forgerock.json.resource.ResourceException.newBadRequestException;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A simple in-memory collection resource provider which uses a {@code Map} to
 * store resources. This resource provider is intended for testing purposes only
 * and there are no performance guarantees.
 */
public final class MemoryBackend implements CollectionResourceProvider {
    private enum FilterResult {
        FALSE, TRUE, UNDEFINED;

        static FilterResult valueOf(final boolean b) {
            return b ? TRUE : FALSE;
        }

        boolean toBoolean() {
            return this == TRUE; // UNDEFINED collapses to FALSE.
        }
    }

    private static final class ResourceComparator implements Comparator<Resource> {
        private final List<SortKey> sortKeys;

        private ResourceComparator(final List<SortKey> sortKeys) {
            this.sortKeys = sortKeys;
        }

        @Override
        public int compare(final Resource r1, final Resource r2) {
            for (final SortKey sortKey : sortKeys) {
                final int result = compare(r1, r2, sortKey);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }

        private int compare(final Resource r1, final Resource r2, final SortKey sortKey) {
            final List<Object> vs1 = getValuesSorted(r1, sortKey.getField());
            final List<Object> vs2 = getValuesSorted(r2, sortKey.getField());
            if (vs1.isEmpty() && vs2.isEmpty()) {
                return 0;
            } else if (vs1.isEmpty()) {
                // Sort resources with missing attributes last.
                return 1;
            } else if (vs2.isEmpty()) {
                // Sort resources with missing attributes last.
                return -1;
            } else {
                // Compare first values only (consistent with LDAP sort control).
                final Object v1 = vs1.get(0);
                final Object v2 = vs2.get(0);
                return sortKey.isAscendingOrder() ? compareValues(v1, v2) : -compareValues(v1, v2);
            }
        }

        private List<Object> getValuesSorted(final Resource resource, final JsonPointer field) {
            final JsonValue value = resource.getContent().get(field);
            if (value == null) {
                return Collections.emptyList();
            } else if (value.isList()) {
                List<Object> results = value.asList();
                if (results.size() > 1) {
                    results = new ArrayList<Object>(results);
                    Collections.sort(results, VALUE_COMPARATOR);
                }
                return results;
            } else {
                return Collections.singletonList(value.getObject());
            }
        }
    }

    private static final QueryFilterVisitor<FilterResult, Resource, JsonPointer> RESOURCE_FILTER =
            new QueryFilterVisitor<FilterResult, Resource, JsonPointer>() {

                @Override
                public FilterResult visitAndFilter(final Resource p,
                        final List<org.forgerock.util.query.QueryFilter<JsonPointer>> subFilters) {
                    FilterResult result = FilterResult.TRUE;
                    for (final org.forgerock.util.query.QueryFilter<JsonPointer> subFilter : subFilters) {
                        final FilterResult r = subFilter.accept(this, p);
                        if (r.ordinal() < result.ordinal()) {
                            result = r;
                        }
                        if (result == FilterResult.FALSE) {
                            break;
                        }
                    }
                    return result;
                }

                @Override
                public FilterResult visitBooleanLiteralFilter(final Resource p, final boolean value) {
                    return FilterResult.valueOf(value);
                }

                @Override
                public FilterResult visitContainsFilter(final Resource p, final JsonPointer field,
                        final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)) {
                            if (valueAssertion instanceof String) {
                                final String s1 =
                                        ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                                final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                                if (s2.contains(s1)) {
                                    return FilterResult.TRUE;
                                }
                            } else {
                                // Use equality matching for numbers and booleans.
                                if (compareValues(valueAssertion, value) == 0) {
                                    return FilterResult.TRUE;
                                }
                            }
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitEqualsFilter(final Resource p, final JsonPointer field,
                        final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)
                                && compareValues(valueAssertion, value) == 0) {
                            return FilterResult.TRUE;
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitExtendedMatchFilter(final Resource p,
                        final JsonPointer field, final String matchingRuleId,
                        final Object valueAssertion) {
                    // This backend does not support any extended filters.
                    return FilterResult.UNDEFINED;
                }

                @Override
                public FilterResult visitGreaterThanFilter(final Resource p,
                        final JsonPointer field, final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)
                                && compareValues(valueAssertion, value) < 0) {
                            return FilterResult.TRUE;
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitGreaterThanOrEqualToFilter(final Resource p,
                        final JsonPointer field, final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)
                                && compareValues(valueAssertion, value) <= 0) {
                            return FilterResult.TRUE;
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitLessThanFilter(final Resource p, final JsonPointer field,
                        final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)
                                && compareValues(valueAssertion, value) > 0) {
                            return FilterResult.TRUE;
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitLessThanOrEqualToFilter(final Resource p,
                        final JsonPointer field, final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)
                                && compareValues(valueAssertion, value) >= 0) {
                            return FilterResult.TRUE;
                        }
                    }
                    return FilterResult.FALSE;
                }

                @Override
                public FilterResult visitNotFilter(final Resource p,
                        final org.forgerock.util.query.QueryFilter<JsonPointer> subFilter) {
                    switch (subFilter.accept(this, p)) {
                    case FALSE:
                        return FilterResult.TRUE;
                    case UNDEFINED:
                        return FilterResult.UNDEFINED;
                    default: // TRUE
                        return FilterResult.FALSE;
                    }
                }

                @Override
                public FilterResult visitOrFilter(final Resource p,
                        final List<org.forgerock.util.query.QueryFilter<JsonPointer>> subFilters) {
                    FilterResult result = FilterResult.FALSE;
                    for (final org.forgerock.util.query.QueryFilter<JsonPointer> subFilter : subFilters) {
                        final FilterResult r = subFilter.accept(this, p);
                        if (r.ordinal() > result.ordinal()) {
                            result = r;
                        }
                        if (result == FilterResult.TRUE) {
                            break;
                        }
                    }
                    return result;
                }

                @Override
                public FilterResult visitPresentFilter(final Resource p, final JsonPointer field) {
                    final JsonValue value = p.getContent().get(field);
                    return FilterResult.valueOf(value != null);
                }

                @Override
                public FilterResult visitStartsWithFilter(final Resource p,
                        final JsonPointer field, final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)) {
                            if (valueAssertion instanceof String) {
                                final String s1 =
                                        ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                                final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                                if (s2.startsWith(s1)) {
                                    return FilterResult.TRUE;
                                }
                            } else {
                                // Use equality matching for numbers and booleans.
                                if (compareValues(valueAssertion, value) == 0) {
                                    return FilterResult.TRUE;
                                }
                            }
                        }
                    }
                    return FilterResult.FALSE;
                }

                private List<Object> getValues(final Resource resource, final JsonPointer field) {
                    final JsonValue value = resource.getContent().get(field);
                    if (value == null) {
                        return Collections.emptyList();
                    } else if (value.isList()) {
                        return value.asList();
                    } else {
                        return Collections.singletonList(value.getObject());
                    }
                }

            };

    private static final Comparator<Object> VALUE_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(final Object o1, final Object o2) {
            return compareValues(o1, o2);
        }
    };

    private static int compareValues(final Object v1, final Object v2) {
        if (v1 instanceof String && v2 instanceof String) {
            final String s1 = (String) v1;
            final String s2 = (String) v2;
            return s1.compareToIgnoreCase(s2);
        } else if (v1 instanceof Number && v2 instanceof Number) {
            final Double n1 = ((Number) v1).doubleValue();
            final Double n2 = ((Number) v2).doubleValue();
            return n1.compareTo(n2);
        } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
            final Boolean b1 = (Boolean) v1;
            final Boolean b2 = (Boolean) v2;
            return b1.compareTo(b2);
        } else {
            // Different types: we need to ensure predictable ordering,
            // so use class name as secondary key.
            return v1.getClass().getName().compareTo(v2.getClass().getName());
        }
    }

    private static boolean isCompatible(final Object v1, final Object v2) {
        return (v1 instanceof String && v2 instanceof String)
                || (v1 instanceof Number && v2 instanceof Number)
                || (v1 instanceof Boolean && v2 instanceof Boolean);
    }

    private final AtomicLong nextResourceId = new AtomicLong();
    private final Map<String, Resource> resources = new ConcurrentHashMap<String, Resource>();
    private final Object writeLock = new Object();

    /**
     * Creates a new in-memory collection containing no resources.
     */
    public MemoryBackend() {
        // No implementation required.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<JsonValue, ResourceException> actionCollection(final ServerContext context,
            final ActionRequest request) {
        try {
            if (request.getAction().equals("clear")) {
                final int size;
                synchronized (writeLock) {
                    size = resources.size();
                    resources.clear();
                }
                final JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
                result.put("cleared", size);
                return newResultPromise(result);
            } else {
                throw new NotSupportedException("Unrecognized action ID '" + request.getAction()
                        + "'. Supported action IDs: clear");
            }
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<JsonValue, ResourceException> actionInstance(final ServerContext context, final String id,
            final ActionRequest request) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        return newExceptionPromise(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> createInstance(final ServerContext context,
            final CreateRequest request) {
        final JsonValue value = request.getContent();
        final String id = request.getNewResourceId();
        final String rev = "0";
        try {
            final Resource resource;
            while (true) {
                final String eid =
                        id != null ? id : String.valueOf(nextResourceId.getAndIncrement());
                final Resource tmp = new Resource(eid, rev, value);
                synchronized (writeLock) {
                    final Resource existingResource = resources.put(eid, tmp);
                    if (existingResource != null) {
                        if (id != null) {
                            // Already exists - put the existing resource back.
                            resources.put(id, existingResource);
                            throw new PreconditionFailedException("The resource with ID '" + id
                                    + "' could not be created because "
                                    + "there is already another resource with the same ID");
                        } else {
                            // Retry with next available resource ID.
                        }
                    } else {
                        // Add succeeded.
                        addIdAndRevision(tmp);
                        resource = tmp;
                        break;
                    }
                }
            }
            return newResultPromise(resource);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> deleteInstance(final ServerContext context, final String id,
            final DeleteRequest request) {
        final String rev = request.getRevision();
        try {
            final Resource resource;
            synchronized (writeLock) {
                resource = getResourceForUpdate(id, rev);
                resources.remove(id);
            }
            return newResultPromise(resource);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> patchInstance(final ServerContext context, final String id,
            final PatchRequest request) {
        final String rev = request.getRevision();
        try {
            final Resource resource;
            synchronized (writeLock) {
                final Resource existingResource = getResourceForUpdate(id, rev);
                final String newRev = getNextRevision(existingResource.getRevision());
                final JsonValue newContent = existingResource.getContent().copy();
                for (final PatchOperation operation : request.getPatchOperations()) {
                    try {
                        if (operation.isAdd()) {
                            newContent.putPermissive(operation.getField(), operation.getValue()
                                    .getObject());
                        } else if (operation.isRemove()) {
                            if (operation.getValue().isNull()) {
                                // Remove entire value.
                                newContent.remove(operation.getField());
                            } else {
                                // Find matching value(s) and remove (assumes reference to array).
                                final JsonValue value = newContent.get(operation.getField());
                                if (value != null) {
                                    if (value.isList()) {
                                        final Object valueToBeRemoved =
                                                operation.getValue().getObject();
                                        final Iterator<Object> iterator = value.asList().iterator();
                                        while (iterator.hasNext()) {
                                            if (valueToBeRemoved.equals(iterator.next())) {
                                                iterator.remove();
                                            }
                                        }
                                    } else {
                                        // Single valued field.
                                        final Object valueToBeRemoved =
                                                operation.getValue().getObject();
                                        if (valueToBeRemoved.equals(value.getObject())) {
                                            newContent.remove(operation.getField());
                                        }
                                    }
                                }
                            }
                        } else if (operation.isReplace()) {
                            newContent.remove(operation.getField());
                            if (!operation.getValue().isNull()) {
                                newContent.putPermissive(operation.getField(), operation.getValue()
                                        .getObject());
                            }
                        } else if (operation.isIncrement()) {
                            final JsonValue value = newContent.get(operation.getField());
                            final Number amount = operation.getValue().asNumber();
                            if (value == null) {
                                throw new BadRequestException("The field '" + operation.getField()
                                        + "' does not exist");
                            } else if (value.isList()) {
                                final List<Object> elements = value.asList();
                                for (int i = 0; i < elements.size(); i++) {
                                    elements.set(i, increment(operation, elements.get(i), amount));
                                }
                            } else {
                                newContent.put(operation.getField(), increment(operation, value
                                        .getObject(), amount));
                            }
                        }
                    } catch (final JsonValueException e) {
                        throw new ConflictException("The field '" + operation.getField()
                                + "' does not exist");
                    }
                }
                resource = new Resource(id, newRev, newContent);
                addIdAndRevision(resource);
                resources.put(id, resource);
            }
            return newResultPromise(resource);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResult, ResourceException> queryCollection(final ServerContext context,
            final QueryRequest request, final QueryResourceHandler handler) {
        if (request.getQueryId() != null) {
            return newExceptionPromise(newNotSupportedException("Query by ID not supported"));
        } else if (request.getQueryExpression() != null) {
            return newExceptionPromise(newNotSupportedException("Query by expression not supported"));
        } else {
            // No filtering or query by filter.
            final org.forgerock.util.query.QueryFilter<JsonPointer> filter = request.getQueryFilter();

            // If paged results are requested then decode the cookie in order to determine
            // the index of the first result to be returned.
            final int pageSize = request.getPageSize();
            final String pagedResultsCookie = request.getPagedResultsCookie();
            final boolean pagedResultsRequested = pageSize > 0;
            final int firstResultIndex;
            if (!pagedResultsRequested || pagedResultsCookie == null
                    || pagedResultsCookie.isEmpty() || request.getPagedResultsOffset() > 0) {
                firstResultIndex = Math.max(0, request.getPagedResultsOffset() - 1);
            } else {
                try {
                    firstResultIndex = Integer.parseInt(pagedResultsCookie);
                } catch (final NumberFormatException e) {
                    return newExceptionPromise(newBadRequestException("Invalid paged results cookie"));
                }
            }
            final int lastResultIndex =
                    pagedResultsRequested ? firstResultIndex + pageSize : Integer.MAX_VALUE;

            // Select, filter, and return the results. These can be streamed if server
            // side sorting has not been requested.
            int resultIndex = 0;
            if (request.getSortKeys().isEmpty()) {
                // No sorting so stream the results.
                for (final Resource resource : resources.values()) {
                    if (filter == null || filter.accept(RESOURCE_FILTER, resource).toBoolean()) {
                        if (resultIndex >= firstResultIndex && resultIndex < lastResultIndex) {
                            handler.handleResource(resource);
                        }
                        resultIndex++;
                    }
                }
            } else {
                // Server side sorting: aggregate the result set then sort. A robust implementation
                // would need to impose administrative limits in order to control memory utilization.
                final List<Resource> results = new ArrayList<Resource>();
                for (final Resource resource : resources.values()) {
                    if (filter == null || filter.accept(RESOURCE_FILTER, resource).toBoolean()) {
                        results.add(resource);
                    }
                }
                Collections.sort(results, new ResourceComparator(request.getSortKeys()));
                for (final Resource resource : results) {
                    if (resultIndex >= firstResultIndex && resultIndex < lastResultIndex) {
                        handler.handleResource(resource);
                    }
                    resultIndex++;
                }
            }

            if (pagedResultsRequested) {
                final String nextCookie =
                        resultIndex > lastResultIndex ? String.valueOf(lastResultIndex) : null;
                final int remaining = Math.max(resultIndex - lastResultIndex, 0);
                return newResultPromise(new QueryResult(nextCookie, remaining));
            } else {
                return newResultPromise(new QueryResult());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> readInstance(final ServerContext context, final String id,
            final ReadRequest request) {
        try {
            final Resource resource = resources.get(id);
            if (resource == null) {
                throw new NotFoundException("The resource with ID '" + id
                        + "' could not be read because it does not exist");
            }
            return newResultPromise(resource);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Resource, ResourceException> updateInstance(final ServerContext context, final String id,
            final UpdateRequest request) {
        final String rev = request.getRevision();
        try {
            final Resource resource;
            synchronized (writeLock) {
                final Resource existingResource = getResourceForUpdate(id, rev);
                final String newRev = getNextRevision(existingResource.getRevision());
                resource = new Resource(id, newRev, request.getContent());
                addIdAndRevision(resource);
                resources.put(id, resource);
            }
            return newResultPromise(resource);
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    /*
     * Add the ID and revision to the JSON content so that they are included
     * with subsequent responses. We shouldn't really update the passed in
     * content in case it is shared by other components, but we'll do it here
     * anyway for simplicity.
     */
    private void addIdAndRevision(final Resource resource) throws ResourceException {
        final JsonValue content = resource.getContent();
        try {
            content.asMap().put(Resource.FIELD_CONTENT_ID, resource.getId());
            content.asMap().put(Resource.FIELD_CONTENT_REVISION, resource.getRevision());
        } catch (final JsonValueException e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON object");
        }
    }

    private String getNextRevision(final String rev) throws ResourceException {
        try {
            return String.valueOf(Integer.parseInt(rev) + 1);
        } catch (final NumberFormatException e) {
            throw new InternalServerErrorException("Malformed revision number '" + rev
                    + "' encountered while updating a resource");
        }
    }

    private Resource getResourceForUpdate(final String id, final String rev)
            throws NotFoundException, PreconditionFailedException {
        final Resource existingResource = resources.get(id);
        if (existingResource == null) {
            throw new NotFoundException("The resource with ID '" + id
                    + "' could not be updated because it does not exist");
        } else if (rev != null && !existingResource.getRevision().equals(rev)) {
            throw new PreconditionFailedException("The resource with ID '" + id
                    + "' could not be updated because " + "it does not have the required version");
        }
        return existingResource;
    }

    private Object increment(final PatchOperation operation, final Object object,
            final Number amount) throws BadRequestException {
        if (object instanceof Long) {
            return ((Long) object) + amount.longValue();
        } else if (object instanceof Integer) {
            return ((Integer) object) + amount.intValue();
        } else if (object instanceof Float) {
            return ((Float) object) + amount.floatValue();
        } else if (object instanceof Double) {
            return ((Double) object) + amount.doubleValue();
        } else {
            throw new BadRequestException("The field '" + operation.getField()
                    + "' is not a number");
        }
    }
}
