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

package org.forgerock.json.resource.http;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.assertj.ActionRequestAssert;
import org.forgerock.json.resource.http.assertj.ActionResponseAssert;
import org.forgerock.json.resource.http.assertj.CreateRequestAssert;
import org.forgerock.json.resource.http.assertj.DeleteRequestAssert;
import org.forgerock.json.resource.http.assertj.JsonContentAssert;
import org.forgerock.json.resource.http.assertj.PatchOperationAssert;
import org.forgerock.json.resource.http.assertj.PatchRequestAssert;
import org.forgerock.json.resource.http.assertj.QueryRequestAssert;
import org.forgerock.json.resource.http.assertj.QueryResponseAssert;
import org.forgerock.json.resource.http.assertj.ReadRequestAssert;
import org.forgerock.json.resource.http.assertj.ResourceExceptionAssert;
import org.forgerock.json.resource.http.assertj.ResourceResponseAssert;
import org.forgerock.json.resource.http.assertj.UpdateRequestAssert;

/**
 * Entry point for assertion methods for different CREST data types.
 * Each method in this class is a static factory for the type-specific assertion objects.
 * The purpose of this class is to make test code more readable.
 *
 * <p>For example:
 *
 * <pre><code>
 *   ResourceResponse response = handler.handleRead(context, request).get();
 *   {@link Assertions#assertThat(JsonValue) assertThat}(response.getContent())
 *         .{@link JsonContentAssert#isEqualTo(JsonValue) isEqualTo}(json(object(field("name", "bjensen"))));
 * </code>
 * </pre>
 */
@SuppressWarnings("javadoc")
public class Assertions {

    /**
     * Creates a new instance of <code>{@link DeleteRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static DeleteRequestAssert assertThat(DeleteRequest actual) {
        return new DeleteRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link ReadRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static ReadRequestAssert assertThat(ReadRequest actual) {
        return new ReadRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link UpdateRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static UpdateRequestAssert assertThat(UpdateRequest actual) {
        return new UpdateRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link PatchRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static PatchRequestAssert assertThat(PatchRequest actual) {
        return new PatchRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link QueryRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static QueryRequestAssert assertThat(QueryRequest actual) {
        return new QueryRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link CreateRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static CreateRequestAssert assertThat(CreateRequest actual) {
        return new CreateRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link ActionRequestAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static ActionRequestAssert assertThat(ActionRequest actual) {
        return new ActionRequestAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link PatchOperationAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static PatchOperationAssert assertThat(PatchOperation actual) {
        return new PatchOperationAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link ResourceResponseAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static ResourceResponseAssert assertThat(ResourceResponse actual) {
        return new ResourceResponseAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link ActionResponseAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static ActionResponseAssert assertThat(ActionResponse actual) {
        return new ActionResponseAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link QueryResponseAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static QueryResponseAssert assertThat(QueryResponse actual) {
        return new QueryResponseAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link ResourceExceptionAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static ResourceExceptionAssert assertThat(ResourceException actual) {
        return new ResourceExceptionAssert(actual);
    }

    /**
     * Creates a new instance of <code>{@link JsonContentAssert}</code>.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    public static JsonContentAssert assertThat(JsonValue actual) {
        return new JsonContentAssert(actual);
    }
}
