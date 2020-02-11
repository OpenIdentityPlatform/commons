# ForgeRock Common HTTP Framework (CHF)

## What is CHF?

CHF (pronounced `chuff`), is the common HTTP framework used throughout ForgeRock products to expose and consume HTTP
APIs in a container/client-agnostic way. This allows other ForgeRock Commons projects to build on top of an
HTTP Framework without having to restrict the products using it to particular architectures - for servers, bindings for
both [Servlet](https://java.net/projects/servlet-spec/) and [Grizzly](https://grizzly.java.net/) are
provided, and for the client, both synchronous and asynchronous client bindings are provided.

The main module for CHF is `chf-http-core`, which contains all the APIs for exposing request handlers, describing them,
consuming them and filtering them. For CHF server applications, one of `chf-http-servlet` or `chf-http-grizzly` are then
bound to the container. For clients, either `chf-client-apache-async` or `chf-client-apache-sync` are added to classpath
depending on the type of operation required. In the case of either client or server, the bindings are not needed to be
dependencies of the modules that provide or consume services - they just have to be included in the final applciation
(and wired in appropriately for servers).

## Getting started

You can quickly try out a [simple CREST HTTP application](json-resource-examples/src/main/java/org/forgerock/json/resource/http/examples)
using the following commands in a terminal:

```
$ git clone https://stash.forgerock.org/scm/commons/forgerock-commons.git
$ cd forgerock-commons
$ mvn clean:install
$ cd http-framework/http-examples/http-servlet-example
$ mvn jetty:run
```

## Implementing an application

To create a CHF application, you must expose an `HttpApplication` which returns a `Handler` that will be used as the
root of the application. A simple example is in the examples:
[ExampleHttpApplication](http-examples/http-servlet-example/src/main/java/org/forgerock/http/servlet/example/ExampleHttpApplication.java)

```
public class ExampleHttpApplication implements HttpApplication {

    @Override
    public Handler start() {
        return new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                Map<String, String> content = new HashMap<>();
                content.put("applicationName", applicationName);
                content.put("matchedUri", context.asContext(UriRouterContext.class).getBaseUri());
                return newResultPromise(new Response(Status.OK).setEntity(content));
            }
        };
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {

    }
}
```

When run, you can then do requests such as:

```
$ curl http://localhost:8080/show/me
{"matchedUri":"","applicationName":"default"}
```

## Describing APIs

In CHF applications, the [OpenAPI Specification](https://github.com/OAI/OpenAPI-Specification/) can be used to describe
the API being exposed, using the
[models](https://github.com/swagger-api/swagger-core/tree/master/modules/swagger-models) module of
[Swagger](http://swagger.io/).

In order to expose API Descriptors in this way, you must do two things:
* Instead of implementing the `HttpApplication` interface, use
[`DescribedHttpApplication`](http-core/src/main/java/org/forgerock/http/DescribedHttpApplication.java)
* Instead of implementing `Handler` for your application, implement
[`DescribableHandler`](http-core/src/main/java/org/forgerock/http/handler/DescribableHandler.java)

The [ExampleHttpApplication](http-examples/http-servlet-example/src/main/java/org/forgerock/http/servlet/example/ExampleHttpApplication.java)
referenced above implements the `DescribedHttpApplication` class, and there is an example handler,
[DescribedOauth2Endpoint](http-examples/http-descriptor-example/src/main/java/org/forgerock/http/example/DescribedOauth2Endpoint.java)
that shows the simple case of how one might expose a static descriptor.

* * *

This README is licensed under a
[Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.](http://creativecommons.org/licenses/by-nc-nd/3.0/)

Copyright 2016 ForgeRock AS.