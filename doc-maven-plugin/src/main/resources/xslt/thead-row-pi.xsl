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

 <!--
      Copy everything, adding a processing instruction into <row>s in <thead>.
 -->

 <!--
      It is critical to specify the namespace of the DocBook elements.
      Otherwise the DocBook elements do not match this template.
 -->
 <xsl:template match="db:thead/db:row">

  <!-- This probably should be a variable in the branding somehow. -->

  <xsl:param name="bgcolor" select="'#EEEEEE'" />

  <row>
   <xsl:processing-instruction name="dbfo">bgcolor="<xsl:value-of select="$bgcolor" />"</xsl:processing-instruction>
   <xsl:apply-templates select="node()|@*"/>
  </row>
 </xsl:template>

 <xsl:template match="node()|@*">
  <xsl:copy>
   <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
 </xsl:template>

</xsl:stylesheet>
