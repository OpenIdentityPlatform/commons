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

package org.forgerock.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allocate a path to a component.
 * <p>
 * This annotation can be applied to either a method or a type:
 * <ul>
 *     <li>
 *         Method - the method should return a handleable type - i.e. either a {@code RequestHandler}, an annotated
 *         POJO, or an implementation of a resource handler interface. This will expose the returned object as a subpath
 *         beneath the handler that the method is a member of.
 *     </li>
 *     <li>
 *         Type - declare the path of a handleable type, so that the path does not have to be declared in a separate
 *         interaction with a router. Note that if the {@code @Path}-annotated handleable type is returned from a method
 *         on another type also annotated with {@code @Path}, then the type annotation is ignored.
 *     </li>
 * </ul>
 * <p>
 * Example:
 * <code><pre>
 *     &#064;RequestHandler(variant = COLLECTION_RESOURCE)
 *     &#064;Path("things")
 *     public class ThingProducer {
 *         &#064;Read
 *         public Promise&lt;ResourceResponse, ResourceException> get(String id) {
 *             // ...
 *         }
 *
 *         &#064;Path("{thing}/subthing")
 *         public SubthingProducer subthing() {
 *             return new SubthingProducer();
 *         }
 *     }
 *
 *     &#064;RequestHandler(variant = SINGLETON_RESOURCE)
 *     public class SubthingProducer {
 *         &#064;Read
 *         public Promise&lt;ResourceResponse, ResourceException> get() {
 *             // ...
 *         }
 *     }
 * </pre></code>
 * In this example, when an instance of {@code ThingProducer} would result in the following paths being created:
 * <ul>
 *     <li>{@code /things} - collection binding to {@code ThingProducer}</li>
 *     <li>{@code /things/{id}} - instance binding to {@code ThingProducer}</li>
 *     <li>{@code /things/{thing}/subthing} - singleton binding to {@code SubthingProducer}</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Path {
    /** The path value. */
    String value();
}
