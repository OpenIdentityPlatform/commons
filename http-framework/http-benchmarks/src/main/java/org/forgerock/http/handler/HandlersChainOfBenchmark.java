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

package org.forgerock.http.handler;

import java.util.concurrent.TimeUnit;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 2, timeUnit = TimeUnit.MINUTES)
@Fork(1)
@Threads(50)
public class HandlersChainOfBenchmark {

    private Promise<Response, NeverThrowsException> OK_RESPONSE =
            Response.newResponsePromise(new Response(Status.OK));

    private Handler OK_HANDLER = new Handler() {
        @Override
        public Promise<Response, NeverThrowsException> handle(final Context context, final Request request) {
            return OK_RESPONSE;
        }
    };

    // use a non-final, non-static variable in order to prevent JVM optimization
    private double x = Math.PI;

    private final Filter PASS_THROUGH = new Filter() {
        @Override
        public Promise<Response, NeverThrowsException> filter(final Context context,
                final Request request,
                final Handler next) {
            // Prevent constant folding by the JVM
            if (x == Double.MAX_VALUE) { return null; }
            return next.handle(context, request);
        }
    };
    private Request REQUEST = new Request();
    private RootContext CONTEXT = new RootContext();

    private Handler original = Handlers.chainOf(OK_HANDLER, PASS_THROUGH, PASS_THROUGH, PASS_THROUGH, PASS_THROUGH, PASS_THROUGH);

    @Benchmark
    public Promise<Response, NeverThrowsException> testOriginalChainOf() {
        return original.handle(CONTEXT, REQUEST);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HandlersChainOfBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
