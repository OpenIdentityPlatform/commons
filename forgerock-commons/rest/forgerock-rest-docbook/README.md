# Common ForgeRock REST DocBook Sources

This module provides DocBook sources used in ForgeRock documentation
for projects that depend on ForgeRock REST libraries (CREST).

To use this module in a documentation project
that builds with `forgerock-doc-maven-plugin`,
unpack the content into shared:

    <plugin>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>unpack-doc-prerequisites</id>
                <phase>pre-site</phase>
                <goals>
                    <goal>unpack</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
        <artifactItems>
            <artifactItem>
                <groupId>org.forgerock.commons</groupId>
                <artifactId>forgerock-rest-docbook</artifactId>
                <version>${forgerockRestVersion}</version>
                <outputDirectory>${project.build.directory}/docbkx-sources</outputDirectory>
            </artifactItem>
        </artifactItems>
        </configuration>
    </plugin>

You can then XInclude the common content in your DocBook documentation:

    <xinclude:include href="../shared/sec-about-crest.xml" />

* * *

The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in compliance with the
License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
specific language governing permission and limitations under the License.

When distributing Covered Software, include this CDDL Header Notice in each file and include
the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
Header, with the fields enclosed by brackets [] replaced by your own identifying
information: "Portions Copyrighted [year] [name of copyright owner]".

Copyright 2015 ForgeRock AS.
