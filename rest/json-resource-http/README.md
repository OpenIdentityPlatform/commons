# ForgeRock Commons REST (CREST) HTTP integration

## What is the CREST HTTP integration module?

For an overview of CREST and its related HTTP protocol please refer to the [README](../README.md) in the parent project.

TODO

Take a look at the [example applications](../json-resource-examples/README.md) for further help.

## Getting started

The CREST modules are defined in the ForgeRock BOM, so you should first import the BOM in your Maven
dependency management section:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.forgerock.commons</groupId>
            <artifactId>forgerock-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then include the appropriate dependencies in your project's dependencies. There's no
need to specify the version:

```xml
  <dependencies>
    <dependency>
        <groupId>org.forgerock.commons</groupId>
        <artifactId>json-resource-http</artifactId>
    </dependency>
</dependencies>
```

* * *

The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in compliance with the
License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
specific language governing permission and limitations under the License.

When distributing Covered Software, include this CDDL Header Notice in each file and include
the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
Header, with the fields enclosed by brackets [] replaced by your own identifying
information: "Portions copyright [year] [name of copyright owner]".

Copyright 2015 ForgeRock AS.
