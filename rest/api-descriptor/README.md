# API DESCRIPTOR

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

### Translation

Some API description fields can be marked for translation, specifically String fields that describe the API.
These String fields can be replaced with a URI. The URI should specify the scheme, the resource location and the key of the text to be used.

* "i18n:relative/path/to/resouce#key"

An example being :

* "i18n:api-descriptor/SnsMessageResource#service.title"

Where the SnsMessageResource.properties is a properties file in the Resources folder, containing the key value pairs like this:

* service.title = Push Notification Response Endpoint

Additional resources can be added next to the default resource which contain desired translations. E.g. Translated French would be in a file called

 * SnsMessageResource_fr.properties

And it would contain the same keys but with French translations, e.g.

* service.title = Je suis désolé, je ne parle pas français

The API descriptor will return the most appropriate translation. If locale is not specified or cannot be matched, the default properties file will be used.
If the locale is specified in the request and is matched, then the resource for that locale is used. The locale should be specified in the request using the 'Accept-Language' header. 

