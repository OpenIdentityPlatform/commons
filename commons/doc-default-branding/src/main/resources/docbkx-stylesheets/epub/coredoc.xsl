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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="1.0">
 <xsl:import href="urn:docbkx:stylesheet" />

 <xsl:param name="html.longdesc" select="0" /> 
 <xsl:param name="variablelist.term.separator"></xsl:param>
 <xsl:param name="variablelist.term.break.after">1</xsl:param>
 <xsl:param name="generate.toc">
  appendix  nop
  article/appendix  nop
  article   nop
  book      toc,title
  chapter   nop
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
  section   nop
  set       toc,title
 </xsl:param>
 <xsl:param name="toc.section.depth" select="0" />
</xsl:stylesheet>