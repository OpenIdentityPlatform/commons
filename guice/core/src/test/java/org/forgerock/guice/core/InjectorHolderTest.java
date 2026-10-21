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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2024-2026 3A Systems LLC.
 */

package org.forgerock.guice.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class InjectorHolderTest {

    private GuiceModuleLoader defaultModuleLoader;
    private PrintStream defaultErr;
    private ByteArrayOutputStream capturedErr;

    @BeforeMethod
    public void setUp() throws Exception {
        defaultErr = System.err;
        capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8.name()));
        defaultModuleLoader = InjectorConfiguration.getGuiceModuleLoader();
        InjectorConfiguration.setGuiceModuleLoader(new TestGuiceModuleLoader());
        InjectorHolder.reset();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        System.setErr(defaultErr);
        InjectorHolder.reset();
        InjectorConfiguration.setGuiceModuleLoader(defaultModuleLoader);
    }

    @Test
    public void shouldAlwaysReturnTheSameInjector() {

        //Given

        //When
        Injector injector = InjectorHolder.getInjector();

        //Then
        Injector sameInjector = InjectorHolder.getInjector();
        assertThat(injector).isEqualTo(sameInjector);
    }

    @Test
    public void shouldGetInstanceWithClass() {

        //Given

        //When
        TestInterface instance = InjectorHolder.getInstance(TestInterface.class);

        //Then
        assertThat(instance).isNotNull().isInstanceOf(TestInterface.class);
    }

    @Test
    public void shouldGetInstanceWithKey() {

        //Given

        //When
        TestInterface instance = InjectorHolder.getInstance(Key.get(TestInterface.class));

        //Then
        assertThat(instance).isNotNull().isInstanceOf(TestInterface.class);
    }

    @Test
    public void shouldInjectMembers() {

        //Given
        TestImplementation testImplementation = new TestImplementation();

        //When
        InjectorHolder.injectMembers(testImplementation);

        //Then
        assertThat(testImplementation.getTestDependency()).isNotNull();
    }

    @Test
    public void shouldCreateChildInjectorOfTheRootInjector() {

        //When
        Injector child = InjectorHolder.createChildInjector();

        //Then
        assertThat(child.getParent()).isSameAs(InjectorHolder.getInjector());
        assertThat(child.getInstance(TestInterface.class)).isInstanceOf(TestImplementation.class);
    }

    @Test
    public void shouldNotBuildTheRootInjectorToReturnTheRegisteredInjector() {

        //Given
        CountingModuleLoader loader = new CountingModuleLoader(TestModule.class);
        InjectorConfiguration.setGuiceModuleLoader(loader);

        //When
        Injector registeredBefore = InjectorHolder.registeredInjector();
        int loaderCallsBefore = loader.calls();
        Injector root = InjectorHolder.getInjector();

        //Then
        assertThat(registeredBefore).isNull();
        assertThat(loaderCallsBefore).isZero();
        assertThat(root).isNotNull();
        assertThat(InjectorHolder.getInjector()).isSameAs(root);
        assertThat(InjectorHolder.registeredInjector()).isNull();
        assertThat(loader.calls()).isEqualTo(1);
    }

    @Test
    public void shouldReportAFailedBuildWithItsCauseToEachLaterCaller() {

        //Given
        FailingModuleLoader loader = new FailingModuleLoader();
        InjectorConfiguration.setGuiceModuleLoader(loader);

        //When
        Throwable first = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));
        InjectorConfiguration.setGuiceModuleLoader(new TestGuiceModuleLoader());
        Throwable second = catchThrowable(() -> InjectorHolder.getInstance(Key.get(TestInterface.class)));

        //Then
        assertThat(first).isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage(FailingModuleLoader.MESSAGE);
        assertThat(second).isInstanceOf(IllegalStateException.class).isNotSameAs(first);
        assertThat(second.getCause()).isSameAs(first.getCause());
        assertThat(loader.calls()).isEqualTo(1);
        assertThat(capturedErr()).containsOnlyOnce(first.getCause().toString());
    }

    @Test
    public void shouldUseARegisteredInjectorDespiteAFailedRootBuild() {

        //Given
        FailingModuleLoader loader = new FailingModuleLoader();
        InjectorConfiguration.setGuiceModuleLoader(loader);
        Throwable failure = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));

        //When
        InjectorHolder.INSTANCE.register(Guice.createInjector(new TestModule()));
        TestInterface instance = InjectorHolder.getInstance(TestInterface.class);
        InjectorHolder.INSTANCE.register(null);
        Throwable afterUnregister = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));

        //Then
        assertThat(instance).isInstanceOf(TestImplementation.class);
        assertThat(afterUnregister).isInstanceOf(IllegalStateException.class);
        assertThat(afterUnregister.getCause()).isSameAs(failure.getCause());
        assertThat(loader.calls()).isEqualTo(1);
        assertThat(capturedErr()).containsOnlyOnce(failure.getCause().toString());
    }

    @Test
    public void shouldKeepTheRootInjectorWhileAnotherInjectorIsRegistered() {

        //Given
        CountingModuleLoader loader = new CountingModuleLoader(TestModule.class);
        InjectorConfiguration.setGuiceModuleLoader(loader);
        Injector root = InjectorHolder.getInjector();
        Injector registered = Guice.createInjector();

        //When
        InjectorHolder.INSTANCE.register(registered);
        Injector whileRegistered = InjectorHolder.getInjector();
        InjectorHolder.INSTANCE.register(null);

        //Then
        assertThat(whileRegistered).isSameAs(registered);
        assertThat(InjectorHolder.getInjector()).isSameAs(root);
        assertThat(loader.calls()).isEqualTo(1);
    }

    @Test(timeOut = 10_000)
    public void shouldBuildTheRootInjectorOnceForConcurrentFirstCalls() throws Exception {

        //Given
        CountDownLatch loaderEntered = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        AtomicInteger loaderCalls = new AtomicInteger();
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> {
            loaderCalls.incrementAndGet();
            loaderEntered.countDown();
            awaitUninterruptibly(releaseLoader);
            return Collections.<Class<? extends Module>>singleton(TestModule.class);
        });
        AtomicReference<Injector> first = new AtomicReference<>();
        AtomicReference<Injector> second = new AtomicReference<>();

        //When
        Thread builder = new Thread(() -> first.set(InjectorHolder.getInjector()));
        builder.start();
        loaderEntered.await();
        Thread waiter = new Thread(() -> second.set(InjectorHolder.getInjector()));
        waiter.start();
        while (waiter.getState() == Thread.State.NEW || waiter.getState() == Thread.State.RUNNABLE) {
            Thread.sleep(5);
        }
        releaseLoader.countDown();
        builder.join();
        waiter.join();

        //Then
        assertThat(loaderCalls).hasValue(1);
        assertThat(first.get()).isNotNull();
        assertThat(second.get()).isSameAs(first.get());
    }

    @Test
    public void shouldBuildTheRootInjectorAgainAfterAnErrorThatIsNotALinkageError() {

        //Given
        AtomicInteger loaderCalls = new AtomicInteger();
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> {
            if (loaderCalls.incrementAndGet() == 1) {
                throw new AssertionError("simulated error");
            }
            return Collections.<Class<? extends Module>>singleton(TestModule.class);
        });

        //When
        Throwable first = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));
        TestInterface instance = InjectorHolder.getInstance(TestInterface.class);

        //Then
        assertThat(first).isInstanceOf(AssertionError.class).hasMessage("simulated error");
        assertThat(instance).isInstanceOf(TestImplementation.class);
        assertThat(loaderCalls).hasValue(2);
    }

    @Test
    public void shouldKeepAFailedModuleClassInitialisationWithItsCause() {

        //Given
        CountingModuleLoader loader = new CountingModuleLoader(FailingStaticInitModule.class);
        InjectorConfiguration.setGuiceModuleLoader(loader);

        //When
        Throwable first = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));
        Throwable second = catchThrowable(() -> InjectorHolder.getInstance(TestInterface.class));

        //Then
        assertThat(first).isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(ExceptionInInitializerError.class)
                .hasRootCauseMessage(FailingStaticInitModule.MESSAGE);
        assertThat(second).isInstanceOf(IllegalStateException.class);
        assertThat(second.getCause()).isSameAs(first.getCause());
        assertThat(loader.calls()).isEqualTo(1);
        assertThat(capturedErr()).containsOnlyOnce(first.getCause().toString());
    }

    private String capturedErr() {
        return new String(capturedErr.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static final class TestGuiceModuleLoader implements GuiceModuleLoader {

        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            Set<Class<? extends Module>> modules = new HashSet<Class<? extends Module>>();
            modules.add(TestModule.class);
            return modules;
        }
    }

    /** Returns the given modules and counts its calls. */
    private static final class CountingModuleLoader implements GuiceModuleLoader {

        private final AtomicInteger calls = new AtomicInteger();
        private final Set<Class<? extends Module>> modules;

        @SafeVarargs
        CountingModuleLoader(Class<? extends Module>... modules) {
            this.modules = new HashSet<>(Arrays.asList(modules));
        }

        @Override
        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            calls.incrementAndGet();
            return modules;
        }

        int calls() {
            return calls.get();
        }
    }

    /** Throws a new exception on each call and counts its calls. */
    private static final class FailingModuleLoader implements GuiceModuleLoader {

        static final String MESSAGE = "no modules on this class path";

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            calls.incrementAndGet();
            throw new UnsupportedOperationException(MESSAGE);
        }

        int calls() {
            return calls.get();
        }
    }

    public static final class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestInterface.class).to(TestImplementation.class);
        }
    }

    /** Its class initialisation fails, so creating it throws ExceptionInInitializerError. */
    public static final class FailingStaticInitModule extends AbstractModule {

        static final String MESSAGE = "module class initialisation failed";

        private static final Object FAILURE = fail();

        private static Object fail() {
            throw new IllegalStateException(MESSAGE);
        }

        @Override
        protected void configure() {
        }
    }

    public static interface TestInterface {

    }

    public static final class TestImplementation implements TestInterface {

        private TestDependency testDependency;

        @Inject
        public void setTestDependency(TestDependency testDependency) {
            this.testDependency = testDependency;
        }

        public TestDependency getTestDependency() {
            return testDependency;
        }
    }

    public static final class TestDependency {

    }
}
