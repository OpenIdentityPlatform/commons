# Overview of Authentication Framework

## Async API Usage:

A **AsyncServerAuthContext** uses a **MessageContext** instance to store state for its own later use and
to pass additional information to other **AsyncServerAuthContext** and **AsyncServerAuthModule** instances
that are configured.

On the other hand a **AsyncServerAuthModule** uses a **MessageContextInfo** instance to store state for
its own later use and to pass additional information to other **AsyncServerAuthContext** and
**AsyncServerAuthModule** instances that are configured.

The reason for these two different objects is to hide information from the **AsyncServerAuthModule**
instances that is only application at the **AsyncServerAuthContext** level. A **MessageContext** is a
**MessageContextInfo** so no conversion needs to take place when passing a **MessageContext** into a call
to a **AsyncServerAuthModule**.

The authentication framework will create a new **MessageContext** instance for each incoming request to
process, which will be passed to the configured **AsyncServerAuthContext**.

---

### Example implementation of a AsyncServerAuthContext
```java
new AsyncServerAuthContext() {

    private final AsyncServerAuthModule module = ...;

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContext context,
            Subject clientSubject, Subject serviceSubject) {
        return module.validateRequest(context, clientSubject, serviceSubject);
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContext context,
            Subject serviceSubject) {
        return module.secureResponse(context, serviceSubject);
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContext context, Subject clientSubject) {
        return module.cleanSubject(context, clientSubject);
    }
};
```

### Example implementation of a AsyncServerAuthModule
```java
new AsyncServerAuthModule() {

    @Override
    public String getModuleId() {
        return "MyFirstModule";
    }

    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy,
            MessagePolicy responsePolicy, CallbackHandler handler, Map<String, Object> options) {
        final PromiseImpl<Void, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(null);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to initialize module", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        HashSet<Class<?>> supportedMessageTypes = new HashSet<Class<?>>();
        supportedMessageTypes.add(Request.class);
        supportedMessageTypes.add(Response.class);
        return supportedMessageTypes;
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageContextInfo messageInfo,
            Subject clientSubject, Subject serviceSubject) {
        final PromiseImpl<AuthStatus, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(AuthStatus.SUCCESS);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to validate request", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageContextInfo messageInfo,
            Subject serviceSubject) {
        final PromiseImpl<AuthStatus, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(AuthStatus.SEND_SUCCESS);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to secure response", e));
                }
            }
        });
        return promise;
    }

    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageContextInfo messageInfo,
            Subject clientSubject) {
        final PromiseImpl<Void, AuthenticationException> promise = PromiseImpl.create();
        ThreadPoolExecutor executor = ...;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Perform some asynchronous processing
                    promise.handleResult(null);
                } catch (Exception e) {
                    promise.handleError(new AuthenticationException("Failed to clean subject", e));
                }
            }
        });
        return promise;
    }
};
```
```