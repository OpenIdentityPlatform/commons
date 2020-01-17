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
 * Copyright 2011-2014 ForgeRock AS
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:d="http://docbook.org/ns/docbook" exclude-result-prefixes="d">
 <xsl:import href="urn:docbkx:stylesheet" />
 <xsl:output method="html" encoding="UTF-8" indent="no" />
 <xsl:preserve-space
 elements="d:computeroutput d:programlisting d:screen d:userinput"/>

 <xsl:template match="d:programlisting">
  <xsl:choose>
   <xsl:when test="@language='aci'">
    <pre class="brush: aci;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='csv'">
    <pre class="brush: csv;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='html'">
    <pre class="brush: html;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='http'">
    <pre class="brush: http;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ini'">
    <pre class="brush: ini;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='java'">
    <pre class="brush: java;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='javascript'">
    <pre class="brush: javascript;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ldif'">
    <pre class="brush: ldif;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='shell'">
    <pre class="brush: shell;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='xml'">
    <pre class="brush: xml;"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:otherwise>
    <pre class="brush: plain;"><xsl:value-of select="." /></pre>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="d:screen">
  <div class="screen">
     <pre>
      <xsl:apply-templates mode="screen"/>
     </pre>
  </div>
 </xsl:template>

 <xsl:template match="*" mode="screen"><xsl:value-of select="."/></xsl:template>

 <xsl:template match="d:replaceable" mode="screen">
  <em><strong><xsl:apply-templates mode="screen"/></strong></em>
 </xsl:template>

 <xsl:template match="d:userinput" mode="screen">
  <strong><xsl:apply-templates mode="screen"/></strong>
 </xsl:template>

 <xsl:template match="d:computeroutput" mode="screen">
  <em><xsl:apply-templates mode="screen"/></em>
 </xsl:template>

 <xsl:param name="make.clean.html" select="1" />
 <xsl:param name="docbook.css.link" select="0" />
 <xsl:param name="docbook.css.source" select="0" />
 <xsl:param name="custom.css.source">coredoc.css.xml</xsl:param>
 <xsl:param name="html.stylesheet">
  css/coredoc.css
  sh/css/shCore.css
  sh/css/shCoreEclipse.css
  sh/css/shThemeEclipse.css
 </xsl:param>
 <xsl:param name="html.script">
  http://code.jquery.com/jquery-1.11.0.min.js
  uses-jquery.js
  sh/js/shCore.js
  sh/js/shBrushAci.js
  sh/js/shBrushBash.js
  sh/js/shBrushCsv.js
  sh/js/shBrushHttp.js
  sh/js/shBrushJava.js
  sh/js/shBrushJScript.js
  sh/js/shBrushLDIF.js
  sh/js/shBrushPlain.js
  sh/js/shBrushProperties.js
  sh/js/shBrushXml.js
  sh/js/shAll.js
 </xsl:param>

 <xsl:param name="admon.style">
  <xsl:value-of select="string('font-style: italic;')"></xsl:value-of>
 </xsl:param>
 <xsl:param name="default.table.frame">none</xsl:param>
 <xsl:param name="default.table.rules">none</xsl:param>
 <xsl:param name="table.cell.border.thickness">0pt</xsl:param>

 <xsl:param name="generate.legalnotice.link" select="1" />
 <xsl:param name="root.filename">index</xsl:param>
 <xsl:param name="use.id.as.filename" select="1" />

 <xsl:param name="generate.toc">
  appendix  toc,title
  article/appendix  nop
  article   nop
  book      toc,title
  chapter   toc,title
  part      toc,title
  preface   nop
  qandadiv  nop
  qandaset  nop
  reference toc,title
  sect1     nop
  sect2     nop
  sect3     nop
  sect4     nop
  sect5     nop
  section   toc
  set       toc,title
 </xsl:param>
 <xsl:param name="generate.section.toc.level" select="1" />
 <xsl:param name="toc.section.depth" select="2" />
 <xsl:param name="toc.max.depth" select="1" />
 <xsl:param name="generate.meta.abstract" select="1" />

 <xsl:param name="use.extensions" select="1" />
</xsl:stylesheet>
