# API Descriptor Example CREST Application

## Introduction

This is a sample CREST application that uses the API Descriptor framework.
In the source code you can find examples how to use the API Descriptor annotations with detailed descriptions.
This readme will guide you how to clone and start up the application and how to send different `curl` commands.
More help about the ForgeRock CREST protocol can be found here: [CREST example readme file](README.md).

## Api Descriptor Example CREST Application Structure

### Model

The application model is based on users and user devices.
Devices can only be added as a user sub-resource.

#### User

name|type|
----|----|
uid | String
username | String
password | String
* | [Devices](Device)  

#### Device

name|type|
----|----|
did | String
name| String
type | String
stolen | boolean
rollOutDate | Date

### Packages

All code is under the ```org.forgerock.json.resource.descriptor.examples``` package.

* The ```handler``` package is where the UserCollectionHandler class lives. It is responsible for 
    creating the version 1.0 and version 2.0 users and device handlers and also the Route to them
* The ```model``` package where the User and Device beans stored
* The ```provider``` package has ```version1``` and ```version2``` sub-packages with the corresponding
    Device and User collection handler classes.

## Cloning and Running the Service

Using the following commands, clone the repository and start the embedded Maven Jetty server with the application:

```
$ git clone ssh://git@stash.forgerock.org:7999/commons/forgerock-commons.git
$ cd forgerock-commons
$ mvn clean:install
$ cd json-resource-examples
$ mvn jetty:run
```


## Available REST Calls

Once the Jetty server is running the following commands are available on the service.

### User Calls

#### Create User (Server-Generated ID)

```
curl -H "Content-Type: application/json" -H "accept-api-version: resource=1.0"  -d '{ "uid" : "user1", "name" : "name1", "password" : "password1" }' 'http://localhost:8080/api/users?_action=create&_prettyPrint=true'
{
  "_id" : "1",
  "_rev" : "0",
  "uid" : "user1",
  "name" : "name1",
  "password" : "password1"
}
```

#### Create User (Client-Supplied ID)

```
curl --request PUT -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '{ "uid" : "5", "username" : "testuser5", "password" : "password5" }' 'http://localhost:8080/api/users/test1?_prettyPrint=true'
{
  "_id" : "test1",
  "_rev" : "0",
  "uid" : "5",
  "username" : "testuser5",
  "password" : "password5"
}
```

#### Read User

```
curl --request GET -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users/test1?_prettyPrint=true'
{
  "_id" : "test1",
  "_rev" : "0",
  "uid" : "5",
  "username" : "testuser5",
  "password" : "password5"
}
```

#### Query Users

##### Query All Users
```
curl -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users?_queryFilter=true&_prettyPrint=true'
{
  "result" : [ {
    "_id" : "0",
    "_rev" : "0",
    "uid" : "user1",
    "name" : "name1",
    "password" : "password1"
  }, {
    "_id" : "1",
    "_rev" : "0",
    "uid" : "user1",
    "name" : "name1",
    "password" : "password1"
  }, {
    "_id" : "test1",
    "_rev" : "0",
    "uid" : "5",
    "username" : "testuser5",
    "password" : "password5"
  } ],
  "resultCount" : 3,
  "pagedResultsCookie" : null,
  "totalPagedResultsPolicy" : "NONE",
  "totalPagedResults" : -1,
  "remainingPagedResults" : -1
}
```

##### List Users Matching Query Filter

```
curl -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users?_queryFilter=uid+eq+"user1"&_fields=username&_prettyPrint=true'
{
  "result" : [ {
    "_id" : "0",
    "_rev" : "0",
    "username" : "testuser1"
  }, {
    "_id" : "test1",
    "_rev" : "3",
    "username" : "testuser1PATCHED"
  } ],
  "resultCount" : 2,
  "pagedResultsCookie" : null,
  "totalPagedResultsPolicy" : "NONE",
  "totalPagedResults" : -1,
  "remainingPagedResults" : -1
}
```

#### Update User

```
curl --request PUT -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '{ "uid": "user1",  "username" : "testuser1UPD", "pasword": "password1" }' 'http://localhost:8080/api/users/test1?_prettyPrint=true'
{
  "_id" : "test1",
  "_rev" : "1",
  "uid" : "user1",
  "username" : "testuser1UPD",
  "password" : "password1"
}
```

#### Patch User

```
curl --request PATCH -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '[{ "operation": "replace", "field": "username", "value": "testuser1PATCHED" }]' 'http://localhost:8080/api/users/test1?_prettyPrint=true'
{
  "_id" : "test1",
  "_rev" : "3",
  "uid" : "user1",
  "password" : "password1",
  "username" : "testuser1PATCHED"
}
```


#### User Action: Reset Password (Not Implemented)

```
curl --request POST -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users/user1?_action=resetPassword&_prettyPrint=true'
{
  "code" : 501,
  "reason" : "Not Implemented",
  "message" : "Action `resetPassword` reached. As it is an example service it has not been implemented."
}
```

### Device Calls

All the above calls need a "user1" uid user to be created. 

#### Create Device (Server-Generated ID)

```
curl -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '{ "did" : "device1", "name" : "devicename1", "type" : "type1" }' 'http://localhost:8080/api/users/user1/devices?_action=create&_prettyPrint=true'
{
  "_id" : "0",
  "_rev" : "0",
  "did" : "device1",
  "name" : "devicename1",
  "type" : "type1"
}
```

#### Create Device (Client-Supplied ID)
```
curl --request PUT -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '{ "did" : "device3", "name" : "devicename3", "type" : "type3" }' 'http://localhost:8080/api/users/user1/devices/dev3?_prettyPrint=true'
{
  "_id" : "dev3",
  "_rev" : "0",
  "did" : "device3",
  "name" : "devicename3",
  "type" : "type3"
}
```

#### Query User's Devices

```
curl -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users/user1/devices?_queryFilter=true&_prettyPrint=true'
{
  "result" : [ {
    "_id" : "0",
    "_rev" : "0",
    "did" : "device1",
    "name" : "devicename1",
    "type" : "type1"
  }, {
    "_id" : "dev3",
    "_rev" : "0",
    "did" : "device3",
    "name" : "devicename3",
    "type" : "type3"
  } ],
  "resultCount" : 2,
  "pagedResultsCookie" : null,
  "totalPagedResultsPolicy" : "NONE",
  "totalPagedResults" : -1,
  "remainingPagedResults" : -1
}
```

#### Delete Device

```
 curl --request DELETE -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users/user1/devices/dev3?_prettyPrint=true'
{
  "_id" : "dev3",
  "_rev" : "0",
  "did" : "device3",
  "name" : "devicename3",
  "type" : "type3"
}
```

#### Update Device

```
curl --request PUT -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '{ "did" : "device1", "name" : "devicename1UPD", "type" : "type1UPD" }' 'http://localhost:8080/api/users/user1/devices/0?_prettyPrint=true'
{
  "_id" : "0",
  "_rev" : "1",
  "did" : "device1",
  "name" : "devicename1UPD",
  "type" : "type1UPD"
}
```

#### Patch Device

```
curl --request PATCH -H "Content-Type: application/json" -H "accept-api-version: resource=1.0" -d '[{ "operation": "replace", "field": "name", "value": "device1PATCHED" }]' 'http://localhost:8080/api/users/user1/devices/0?_prettyPrint=true'
{
  "_id" : "0",
  "_rev" : "2",
  "did" : "device1",
  "type" : "type1UPD",
  "name" : "device1PATCHED"
}
```

#### Device Action: Mark as Stolen (Not Implemented)

```
curl --request POST -H "accept-api-version: resource=1.0" 'http://localhost:8080/api/users/user1/devices/0?_action=markAsStolen&_prettyPrint=true'
{
  "code" : 501,
  "reason" : "Not Implemented",
  "message" : "Action `markAsStolen` reached. As it is an example service it has not been implemented."
}
```

#### Device Action: Roll Out (Not Implemented, but Supported in Version 2.0)

```
curl --request POST -H "accept-api-version: resource=2.0" 'http://localhost:8080/api/users/user1/devices/0?_action=rollOut&_prettyPrint=true'
{
  "code" : 501,
  "reason" : "Not Implemented",
  "message" : "Action `rollOut` reached. As it is an example service it has not been implemented."
}
```
