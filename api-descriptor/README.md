# API DESCRIPTOR

Related to https://bugster.forgerock.org/jira/browse/RAPID-5

### Description

As both ForgeRock customers and internal developers increasingly favor REST APIs to access the ForgeRock Identity Platform, they all depend on clear descriptions of the API contracts. Today, we maintain descriptions by hand through reverse engineering. We lack ways of ensuring this hand-written documentation is updated when the implementations change, which is an impediment to internal development. Furthermore, our products lack the ability to describe the APIs as they are exposed at runtime in deployment and hence make it harder for community members and customers to use our APIs.

### Benefits

There are multiple benefits of a well-functioning API Descriptor, both for ForgeRock Engineering as well as ForgeRock customers and community members:

* Enablement of developers to more easily understand and use ForgeRock APIs.
* Ability for developers to test APIs before using them.
* Ability to dynamically generate API documentation.
* Ability to dynamically create user interfaces that are built from API calls.

### Scenarios

* Community members or customer prospects evaluate ForgeRock products for easy of use and interoperability.
* Customers investigate ways to maximize the use of, and potentially extend, the Platform to fit their business needs.
* Prospects and customers test expected behaviour of ForgeRock functionality.
* Developers get access to API descriptions only of those that they are allowed to use.
* Developers get access to SDKs that are automatically generated from the API Descriptor.
* The ForgeRock documentation team generates and maintains API documentation.
* The ForgeRock UI teams develop new interfaces.

### Features list

**Highlights:**

* A specification that clearly defines how to describe our APIs
* Tools to build and generate API descriptors from our code
* Extensions for Swagger
* Endpoints to filter content for API descriptions
* Endpoints to discover API descriptions
* Automatic generation of API documentation
* A ForgeRock-consistent UI to explore the APIs

### Additional Resources

**Static documentation by other companies:**

* Oracle seems to offer very static documentation. Example: Oracle  Fusion Middleware Developing and Customizing Applications for Oracle Identity Manager - Using SCIM/REST Services: https://docs.oracle.com/cd/E52734_01/oim/OMDEV/scim.htm#OMDEV5526
* Ping seems to have very static documentation only. Example: https://developer.pingidentity.com/en/api/pingid-api.html
* Gigya has very good, nicely laid out documentation, but also static: http://developers.gigya.com/display/GD/REST+API
* Okta recommends Postman as the API Test Client, and provides environment templates for it: http://developer.okta.com/docs/api/getting_started/api_test_client.html

**Examples of dynamic API Descriptors:**

* A sample demo of how a Swagger UI-driven Descriptor can look like: http://petstore.swagger.io/#/
* Wordnik uses Swagger for their API doc: http://developer.wordnik.com/docs.html
* Twitter has a API Console Tool, that seems to be the dynamic, Swagger-like tool that we want to build: https:/dev.twitter.com/rest/tools/console
(Note: powered by Apigee)

### Customer Feedback

(Warren:) SDK generation, in as many languages as possible, is the number one reason to implement an API Descriptor. Customers do more and more DevOps work in **Go**, like many others. Most developers do not want to read REST docs. They want to use a library that does it for them.