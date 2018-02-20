<?xml version="1.0" encoding="UTF-8"?>
<!--
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
 * Copyright 2015 ForgeRock AS.
-->
<xsl:stylesheet
 version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:db="http://docbook.org/ns/docbook"
 exclude-result-prefixes="db">
 <xsl:output method="xml" />

 <xsl:template match="db:programlisting|db:screen">
  <xsl:copy>
   <!-- Less than 30 lines? Try to keep it on the same page in PDF. -->
   <!-- xsltproc does not seem to accept < in attribute values. -->
   <xsl:if test="not(string-length(node()) - string-length(translate(string(node()), '&#x0A;', '')) > 30)">
    <xsl:processing-instruction name="dbfo">keep-together="always"</xsl:processing-instruction>
   </xsl:if>
   <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
 </xsl:template>

 <xsl:template match="node()|@*">
  <xsl:copy>
   <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
 </xsl:template>

</xsl:stylesheet>
