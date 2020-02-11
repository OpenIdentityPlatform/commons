<?xml version="1.0" encoding="UTF-8"?>
<!--
! MPL 2.0 HEADER START
!
! This Source Code Form is subject to the terms of the Mozilla Public
! License, v. 2.0. If a copy of the MPL was not distributed with this
! file, You can obtain one at http://mozilla.org/MPL/2.0/.
!
! If applicable, add the following below this MPL 2.0 HEADER, replacing
! the fields enclosed by brackets "[]" replaced with your own identifying
! information:
!     Portions Copyright [yyyy] [name of copyright owner]
!
! MPL 2.0 HEADER END
!
!     Copyright 2013-2015 ForgeRock AS.
!
-->
<xsl:stylesheet
 version="1.0"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:db="http://docbook.org/ns/docbook"
 exclude-result-prefixes="db">
 <xsl:output method="xml" />

 <!--
      Copy everything, making sure DocBook ImageData elements have attributes:
      <imagedata  ... scalefit="1" width="100%" contentdepth="100%"/>
 -->

 <!--
      It is critical to specify the namespace of the ImageData element.
      Otherwise the DocBook ImageData element does not match this template.
 -->
 <xsl:template match="db:imagedata">
  <xsl:element name="imagedata" namespace="http://docbook.org/ns/docbook">
   <xsl:attribute name="align">center</xsl:attribute>
   <xsl:attribute name="scalefit">1</xsl:attribute>
   <xsl:attribute name="width">100%</xsl:attribute>
   <xsl:attribute name="contentdepth">100%</xsl:attribute>
   <xsl:apply-templates select="node()|@*"/>
  </xsl:element>
 </xsl:template>

 <xsl:template match="node()|@*">
  <xsl:copy>
   <xsl:apply-templates select="node()|@*"/>
  </xsl:copy>
 </xsl:template>

</xsl:stylesheet>
