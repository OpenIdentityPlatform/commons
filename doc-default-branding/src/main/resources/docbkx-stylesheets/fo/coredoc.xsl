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
 * Copyright 2011-2015 ForgeRock AS.
-->
<xsl:stylesheet
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:d="http://docbook.org/ns/docbook"
xmlns:fo="http://www.w3.org/1999/XSL/Format"
xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"
exclude-result-prefixes="d"
version="1.0">

 <xsl:import href="urn:docbkx:stylesheet"/>
 <xsl:import href="forgerocktitlepage.xsl"/>
 <xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>

<!--  =====================================================================  -->
<!--                          Page Settings                                  -->
<!--  =====================================================================  -->

  <xsl:param name="page.height.portrait">9in</xsl:param>
  <xsl:param name="page.width.portrait">7.5in</xsl:param>

  <xsl:param name="page.margin.inner">0.75in</xsl:param>

  <!--  Testing Single.sided  -->
  <xsl:param name="double.sided" select="1" />

  <xsl:param name="draft.mode" select="no" />

<!--  =====================================================================  -->
<!--                         FOP Extensions                                  -->
<!--  =====================================================================  -->

  <xsl:param name="fop1.extensions" select="1"/>

<!--  =====================================================================  -->
<!--                         Default Text Alignment                          -->
<!--  =====================================================================  -->

  <xsl:param name="alignment">left</xsl:param>

<!--  =====================================================================  -->
<!--                      XSL:FO Default Properties                          -->
<!--  =====================================================================  -->

 <!-- DOCS-72  Improve widow and orphans control in PDF  -->
 <!--                use: <?hard-pagebreak?>             -->
 <xsl:template match="processing-instruction('hard-pagebreak')">
  <fo:block break-after='page'/>
 </xsl:template>

 <!--      linebreak   -->
 <!-- use <?linebreak?> -->
 <xsl:template match="processing-instruction('linebreak')">
  <fo:block/>
 </xsl:template>


<!--  =====================================================================  -->
<!--                                Fonts                                    -->
<!--  =====================================================================  -->


  <xsl:param name="body.font.master">9</xsl:param>

  <xsl:param name="body.font.family">DejaVuSerif</xsl:param>
  <xsl:param name="dingbat.font.family">DejaVuSerif</xsl:param>
  <xsl:param name="monospace.font.family">DejaVuSansMono</xsl:param>

  <!--- Using Universe instead of DejaVuSans -->
  <xsl:param name="sans.font.family">DejaVuSans</xsl:param>
  <xsl:param name="title.font.family">DejaVuSans</xsl:param>

<!--  =====================================================================  -->
<!--                         Section Headings                                -->
<!--  =====================================================================  -->

 <!-- DOCS-142 -->
 <xsl:attribute-set name="section.title.properties">
  <xsl:attribute name="space-before">14pt</xsl:attribute>
 </xsl:attribute-set>

 <!-- DOCS-171 -->
 <xsl:attribute-set name="section.title.level1.properties">
  <xsl:attribute name="space-before">22pt</xsl:attribute>
  <xsl:attribute name="font-size">15pt</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="section.title.level2.properties">
  <xsl:attribute name="space-before">18pt</xsl:attribute>
  <xsl:attribute name="font-size">11pt</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="section.title.level3.properties">
  <xsl:attribute name="space-before">16pt</xsl:attribute>
  <xsl:attribute name="font-size">11pt</xsl:attribute>
 </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level4.properties">
   <xsl:attribute name="space-before">16pt</xsl:attribute>
   <xsl:attribute name="font-size">10pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level5.properties">
   <xsl:attribute name="font-size">14pt</xsl:attribute>
   <xsl:attribute name="font-size">10pt</xsl:attribute>
  </xsl:attribute-set>
  
  <xsl:param name="generate.toc">
    appendix  nop
    article/appendix  nop
    article   nop
    book      toc,title
    chapter   nop
    part      nop
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
  <xsl:param name="toc.max.depth">2</xsl:param>
  
  <xsl:param name="use.extensions" select="1"/>
  <xsl:param name="linenumbering.everyNth" select="1"/>
  <xsl:param name="orderedlist.label.width">1.8em</xsl:param>
  
  <xsl:param name="variablelist.as.blocks" select="1" />
  <xsl:param name="variablelist.term.separator" select="''"/>
  <xsl:param name="variablelist.term.break.after">1</xsl:param>
  
  <xsl:attribute-set name="monospace.properties">
   <xsl:attribute name="font-size">0.9em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:param name="shade.verbatim" select="1"/>

  <xsl:attribute-set name="shade.verbatim.style">
   <xsl:attribute name="background-color">#d4d4d4</xsl:attribute>
   <xsl:attribute name="border">0.5pt dashed #626d75</xsl:attribute>
   <xsl:attribute name="padding">3pt</xsl:attribute>
   <xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
   <xsl:attribute name="font-size">0.75em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:param name="ulink.footnotes" select="0"/>
  <xsl:param name="ulink.show" select="0"/>

<!--  =====================================================================  -->
<!--                            Hyphenation                                  -->
<!--  =====================================================================  -->

 <!-- Hyphenate URLs in running text at ? and &. -->
 <xsl:param name="ulink.hyphenate">&#x200B;</xsl:param><!-- Zero-width space -->
 <xsl:param name="ulink.hyphenate.chars">?&amp;</xsl:param>

 <!--
      Hyphenate long literals at literal.hyphenate.chars.
      Adapted from the hyphenate-url template.
 -->
  <xsl:param name="literal.hyphenate">&#x200B;</xsl:param><!-- Zero-width space -->
  <xsl:param name="literal.hyphenate.chars">./,-?&amp;</xsl:param>

  <xsl:template match="d:literal//text()">
    <xsl:call-template name="hyphenate-literal">
     <xsl:with-param name="literal" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="hyphenate-literal">
    <xsl:param name="literal" select="''"/>
    <xsl:choose>
      <xsl:when test="string-length($literal) &gt; 1">
        <xsl:variable name="char" select="substring($literal, 1, 1)"/>
        <xsl:value-of select="$char"/>
        <xsl:if test="contains($literal.hyphenate.chars, $char)">
          <!-- Do not hyphen in-between // -->
          <xsl:if test="not($char = '/' and substring($literal,2,1) = '/')">
            <xsl:copy-of select="$literal.hyphenate"/>
          </xsl:if>
        </xsl:if>
        <!-- recurse to the next character -->
        <xsl:call-template name="hyphenate-literal">
          <xsl:with-param name="literal" select="substring($literal, 2)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$literal"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--
       Wrap screen text containing HTTP* URLs at the start of the query string.
  -->
  <xsl:template match="d:screen//text()">
   <xsl:call-template name="split-screen-lines">
    <xsl:with-param name="lines" select="." />
   </xsl:call-template>
  </xsl:template>

  <xsl:template name="split-screen-lines">
   <!-- Inspired by http://www.heber.it/?p=1088 -->
   <xsl:param name="lines" />
   <xsl:param name="eol" select="'&#x000A;'" />

   <xsl:choose>

    <xsl:when test="contains($lines, $eol)">
     <!-- Handle everything up to the first EOL. -->
     <xsl:call-template name="string-replace">
      <xsl:with-param name="text" select="concat(substring-before($lines, $eol), $eol)" />
      <xsl:with-param name="replace" select="'?'" />
      <xsl:with-param name="by" select="'&#x000A; ?'" />
     </xsl:call-template>

     <!-- Handle everything that remains. -->
     <xsl:call-template name="split-screen-lines">
      <xsl:with-param name="lines" select="substring-after($lines, $eol)" />
     </xsl:call-template>
    </xsl:when>

    <xsl:otherwise>
     <xsl:choose>
      <xsl:when test="$lines = ''">
       <xsl:text />
      </xsl:when>

      <xsl:otherwise>
       <xsl:call-template name="string-replace">
        <xsl:with-param name="text" select="$lines" />
        <xsl:with-param name="replace" select="'?'" />
        <xsl:with-param name="by" select="'&#x000A; ?'" />
       </xsl:call-template>
      </xsl:otherwise>
     </xsl:choose>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:template>

  <xsl:template name="string-replace">
   <xsl:param name="text" />
   <xsl:param name="replace" />
   <xsl:param name="by" />
   <xsl:param name="max-length" select="80" />
   <xsl:choose>
     <xsl:when test="string-length($text) &gt; $max-length
                     and contains($text, 'http') and contains($text, $replace)">
       <xsl:value-of select="substring-before($text, $replace)" />
       <xsl:value-of select="$by" />
       <xsl:call-template name="string-replace">
         <xsl:with-param name="text"
                         select="substring-after($text, $replace)" />
         <xsl:with-param name="replace" select="$replace" />
         <xsl:with-param name="by" select="$by" />
       </xsl:call-template>
     </xsl:when>
     <xsl:otherwise>
       <xsl:value-of select="$text" />
     </xsl:otherwise>
   </xsl:choose>
  </xsl:template>

  <!-- Do not hyphenate in general. -->
  <xsl:param name="hyphenate">false</xsl:param>


<!--  =====================================================================  -->
<!--                            Admonitions                                  -->
<!--  =====================================================================  -->

 <!-- DOCS-137 -->
 <xsl:param name="admon.graphics" select="1" />
 <xsl:param name="admon.graphics.path"
            select="'../shared/images/'" />
 <xsl:param name="admon.graphics.extension">.png</xsl:param>

 <xsl:template match="*" mode="admon.graphic.width">
  <xsl:text>32pt</xsl:text>
 </xsl:template>

 <xsl:attribute-set name="admonition.properties">
  <xsl:attribute name="border-top">0.5pt solid black</xsl:attribute>
  <xsl:attribute name="border-bottom">0.5pt solid black</xsl:attribute>
  <xsl:attribute name="padding-top">2pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">4pt</xsl:attribute>
  <xsl:attribute name="margin-bottom">12pt</xsl:attribute>
  <xsl:attribute name="margin-right">10mm</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="admonition.title.properties">
  <xsl:attribute name="font-family">Arial</xsl:attribute>
  <xsl:attribute name="font-size">12pt</xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="margin-top">12pt</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="graphical.admonition.properties">
  <xsl:attribute name="space-before.optimum">1.4em</xsl:attribute>
  <xsl:attribute name="space-before.minimum">1.2em</xsl:attribute>
  <xsl:attribute name="space-before.maximum">1.6em</xsl:attribute>
 </xsl:attribute-set>


<!--  =====================================================================  -->
<!--                   Monospace Verbatim Font Width                         -->
<!--  =====================================================================  -->

  <!-- DOCS-75: Wide programlisting shading extends to the right edge of the page in PDF -->
  <xsl:param name="monospace.verbatim.font.width">0.445em</xsl:param>


<!--  =====================================================================  -->
<!--                        Table Cell Padding                               -->
<!--  =====================================================================  -->

  <!-- DOCS-86: Leave more space between table cells in PDF -->
  <xsl:attribute-set name="table.cell.padding">
    <xsl:attribute name="padding-left">8pt</xsl:attribute>
    <xsl:attribute name="padding-right">8pt</xsl:attribute>
    <xsl:attribute name="padding-top">2pt</xsl:attribute>
    <xsl:attribute name="padding-bottom">2pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="xref.properties">
    <xsl:attribute name="color">#47a</xsl:attribute>
  </xsl:attribute-set>


<!--  =====================================================================  -->
<!--                     Chapter Headings                                    -->
<!--  =====================================================================  -->

 <!-- DOCS-146: Fix Chapter Headings -->
 <!-- This snippet customizes the generated 'Chapter N' to just 'N' -->
 <xsl:param name="local.l10n.xml" select="document('')"/>
 <l:i18n>
  <l:l10n language="en">
   <l:context name="title-numbered">
    <l:template name="chapter" text="%n %t" />
   </l:context>
  </l:l10n>
 </l:i18n>

 <xsl:template name="chap.title">
  <xsl:param name="node" select="."/>

  <fo:block xsl:use-attribute-sets="chap.label.properties">
   <xsl:call-template name="gentext">
    <xsl:with-param name="key">
     <xsl:choose>
      <xsl:when test="$node/self::chapter">chapter</xsl:when>
      <xsl:when test="$node/self::appendix">appendix</xsl:when>
      <xsl:when test="$node/self::glossary">glossary</xsl:when>
      <xsl:when test="$node/self::index">index</xsl:when>
     </xsl:choose>
    </xsl:with-param>
   </xsl:call-template>
   <xsl:text>Chapter </xsl:text>
   <xsl:apply-templates select="$node" mode="label.markup"/>
  </fo:block>
  <fo:block xsl:use-attribute-sets="chap.title.properties">
   <xsl:apply-templates select="$node" mode="title.markup"/>
  </fo:block>
 </xsl:template>

 <xsl:attribute-set name="chap.label.properties">
  <xsl:attribute name="font-size">10pt</xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="text-align">left</xsl:attribute>
  <xsl:attribute name="margin-top">2.0in</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="chap.title.properties">
  <xsl:attribute name="font-size">20pt</xsl:attribute>
  <xsl:attribute name="text-align">left</xsl:attribute>
  <xsl:attribute name="space-after">0.5in</xsl:attribute>
 </xsl:attribute-set>

<!--  =====================================================================  -->
<!--                          Body Indent                                    -->
<!--  =====================================================================  -->

 <!-- DOCS-152: Fix Subheading Horizontal Spacing  -->
 <!-- This really fixes the subheadings  -->
 <xsl:param name="body.start.indent">20mm</xsl:param>


<!--  =====================================================================  -->
<!--                          Section Headings                               -->
<!--  =====================================================================  -->


 <!--  This section is for the subheadings -->
 <xsl:template name="section.heading">
  <xsl:param name="level" select="1"/>
  <xsl:param name="marker" select="1"/>
  <xsl:param name="title"/>
  <xsl:param name="marker.title"/>

  <xsl:variable name="title.block">
   <fo:list-block start-indent="0mm"
                  provisional-distance-between-starts="{$body.start.indent}"
                  provisional-label-separation="5mm">
    <fo:list-item>
     <fo:list-item-label end-indent="label-end()" text-align="start">
      <fo:block>
       <xsl:apply-templates select="parent::*" mode="label.markup"/>
      </fo:block>
     </fo:list-item-label>
     <fo:list-item-body start-indent="body-start()" text-align="start">
      <fo:block>
       <xsl:apply-templates select="parent::*" mode="title.markup"/>
      </fo:block>
     </fo:list-item-body>
    </fo:list-item>
   </fo:list-block>
  </xsl:variable>

  <fo:block xsl:use-attribute-sets="section.title.properties">
   <xsl:if test="$marker != 0">
    <fo:marker marker-class-name="section.head.marker">
     <xsl:copy-of select="$marker.title"/>
    </fo:marker>
   </xsl:if>

   <xsl:choose>
    <xsl:when test="$level=1">
     <fo:block xsl:use-attribute-sets="section.title.level1.properties">
      <xsl:copy-of select="$title.block"/>
     </fo:block>
    </xsl:when>
    <xsl:when test="$level=2">
     <fo:block xsl:use-attribute-sets="section.title.level2.properties">
      <xsl:copy-of select="$title.block"/>
     </fo:block>
    </xsl:when>
    <xsl:when test="$level=3">
     <fo:block xsl:use-attribute-sets="section.title.level3.properties">
      <xsl:copy-of select="$title.block"/>
     </fo:block>	 
    </xsl:when>
    <xsl:when test="$level=4">
     <fo:block xsl:use-attribute-sets="section.title.level4.properties">
      <xsl:copy-of select="$title.block"/>
     </fo:block>	 
    </xsl:when>	
    <xsl:otherwise>
     <fo:block xsl:use-attribute-sets="section.title.properties"/>
    </xsl:otherwise>
   </xsl:choose>

  </fo:block>
 </xsl:template>

<!--  =====================================================================  -->
<!--                         Book Title Page                                 -->
<!--  =====================================================================  -->

 <!-- DOCS-28 Fix PDF Cover Page  -->
 <xsl:template name="book.titlepage.recto" >

 <fo:block-container>
  <fo:block >
   <fo:table inline-progression-dimension="100%" table-layout="fixed"
             border-collapse="collapse">

    <fo:table-body>

     <!-- Mediaobject -->
     <fo:table-row>
      <fo:table-cell>
       <fo:block xsl:use-attribute-sets="book.titlepage.mediaobject.recto.style">
        <xsl:choose>
         <xsl:when test="d:info/d:mediaobject">
          <xsl:apply-templates
           mode="book.titlepage.recto.auto.mode"
           select="d:info/d:mediaobject"/>
         </xsl:when>
        </xsl:choose>
       </fo:block>
      </fo:table-cell>
     </fo:table-row>

     <fo:table-row>
      <fo:table-cell>
         <fo:block xsl:use-attribute-sets="book.titlepage.title.recto.style">
          <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:title"/>
         </fo:block>
      </fo:table-cell>
     </fo:table-row>

     <fo:table-row>
      <fo:table-cell>
         <fo:block xsl:use-attribute-sets="book.titlepage.subtitle.recto.style">
          <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:subtitle"/>
         </fo:block>
      </fo:table-cell>
     </fo:table-row>

     <fo:table-row>
      <fo:table-cell xsl:use-attribute-sets="book.titlepage.authorgroup.recto.style">

       <xsl:for-each select="d:info/d:authorgroup/d:author/d:personname">
        <fo:block>
         <xsl:apply-templates/>
        </fo:block>
       </xsl:for-each>

      <fo:block-container absolute-position="fixed" height="30mm" width="50mm" left="121.5mm" top="182.5mm">
       <fo:block>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:orgname"/>
       </fo:block>
       <fo:block >
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:street"/>
        <xsl:text>, </xsl:text>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:otheraddr"/>
       </fo:block>
       <fo:block >
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:city"/>
        <xsl:text>, </xsl:text>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:state"/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:postcode"/>
        <xsl:text>, </xsl:text>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:country"/>
       </fo:block>
       <fo:block>
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:phone"/>
       </fo:block>
       <fo:block >
        <xsl:apply-templates mode="book.titlepage.recto.mode" select="d:info/d:authorgroup/d:author/d:affiliation/d:address/d:uri"/>
       </fo:block>
      </fo:block-container>

      </fo:table-cell>
     </fo:table-row>
    </fo:table-body>
   </fo:table>
  </fo:block>
 </fo:block-container>
</xsl:template>

<xsl:attribute-set name="book.titlepage.mediaobject.recto.style">
 <xsl:attribute name="margin-top">80px</xsl:attribute>
 <xsl:attribute name="margin-left">-11.3cm</xsl:attribute>
 <xsl:attribute name="text-align">left</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="book.titlepage.title.recto.style">
 <xsl:attribute name="font-family">DejaVuSans</xsl:attribute>
 <xsl:attribute name="font-size">16pt</xsl:attribute>
 <xsl:attribute name="margin-left">20px</xsl:attribute>
 <xsl:attribute name="text-align">left</xsl:attribute>
</xsl:attribute-set>

 <xsl:attribute-set name="book.titlepage.subtitle.recto.style">
  <xsl:attribute name="font-family">DejaVuSans</xsl:attribute>
  <xsl:attribute name="font-size">9pt</xsl:attribute>
  <xsl:attribute name="margin-left">20px</xsl:attribute>
  <xsl:attribute name="text-align">left</xsl:attribute>
  <xsl:attribute name="margin-bottom">1.0in</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="book.titlepage.authorgroup.recto.style">
  <xsl:attribute name="font-family">DejaVuSans</xsl:attribute>
  <xsl:attribute name="font-size">8pt</xsl:attribute>
  <xsl:attribute name="text-align">right</xsl:attribute>
  <xsl:attribute name="margin-right">20px</xsl:attribute>
 </xsl:attribute-set>

 <!--  =====================================================================  -->
 <!--                         TOC Page                                        -->
 <!--  =====================================================================  -->


 <xsl:attribute-set name="toc.margin.properties">
  <xsl:attribute name="start-indent">0.25in</xsl:attribute>
 </xsl:attribute-set>

 <xsl:attribute-set name="toc.line.properties">
  <xsl:attribute name="text-align-last">justify</xsl:attribute>
  <xsl:attribute name="text-align">justify</xsl:attribute>
  <xsl:attribute name="end-indent">0.75in</xsl:attribute>
  <xsl:attribute name="last-line-end-indent">-0.25in</xsl:attribute>
 </xsl:attribute-set>

 <!--  =====================================================================  -->
 <!--                      Table, Figure, Example Titles                      -->
 <!--  =====================================================================  -->

 <xsl:attribute-set name="formal.title.properties">
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="font-size">9pt</xsl:attribute>
  <xsl:attribute name="hyphenate">false</xsl:attribute>
  <xsl:attribute name="font-family">Helvetica</xsl:attribute>
  <xsl:attribute name="space-after">0pt</xsl:attribute>
  <xsl:attribute name="margin-top">14pt</xsl:attribute>
 </xsl:attribute-set>

 <!--  =====================================================================  -->
 <!--                      Headers and Footers                                -->
 <!--  =====================================================================  -->

 <!-- DOCS-160 -->
 <xsl:attribute-set name="header.content.properties">
  <xsl:attribute name="font-size">8pt</xsl:attribute>
 </xsl:attribute-set>

 <!--  =====================================================================  -->
 <!--                             Tables                                      -->
 <!--  =====================================================================  -->

 <!--xsl:param name="default.table.frame">topbot</xsl:param>
 <xsl:param name="default.table.rules">rows</xsl:param>
 <xsl:param name="table.cell.border.thickness">0pt</xsl:param-->

 <!-- general cell padding for all tables -->
 <xsl:attribute-set name="table.cell.padding">
  <xsl:attribute name="padding-top">5pt</xsl:attribute>
  <xsl:attribute name="padding-bottom">3pt</xsl:attribute>
 </xsl:attribute-set>

 <!-- Border settings -->
 <xsl:param name="table.cell.border.color">#D3D2D1</xsl:param>
 <xsl:param name="table.cell.border.thickness">1pt</xsl:param>
 <xsl:param name="table.frame.border.color">#D3D2D1</xsl:param>
 <xsl:param name="table.frame.border.thickness">1pt</xsl:param>

 <xsl:template name="table.row.properties">
  <xsl:variable name="row-height">
   <xsl:if test="processing-instruction('dbfo')">
    <xsl:call-template name="pi.dbfo_row-height"/>
   </xsl:if>
  </xsl:variable>
  <xsl:if test="$row-height != ''">
   <xsl:attribute name="block-progression-dimension">
    <xsl:value-of select="$row-height"/>
   </xsl:attribute>
  </xsl:if>
  <xsl:variable name="bgcolor">
   <xsl:call-template name="pi.dbfo_bgcolor"/>
  </xsl:variable>
  <xsl:if test="$bgcolor != ''">
   <xsl:attribute name="background-color">
    <xsl:value-of select="$bgcolor"/>
   </xsl:attribute>
  </xsl:if>
  <!--  Keep header row with next row-->
  <xsl:if test="ancestor::thead">
   <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
   <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:if>
 </xsl:template>

 <!--  =====================================================================  -->
 <!--                            literallayout                                -->
 <!--  =====================================================================  -->

 <xsl:template match="d:literallayout">
  <xsl:param name="suppress-numbers" select="'0'"/>

  <!-- This is the only line added to the template -->
  <xsl:param name="shade.verbatim" select ="'0'"/>

  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>

  <xsl:variable name="keep.together">
   <xsl:call-template name="pi.dbfo_keep-together"/>
  </xsl:variable>

  <xsl:variable name="content">
   <xsl:choose>
    <xsl:when test="$suppress-numbers = '0'
                      and @linenumbering = 'numbered'
                      and $use.extensions != '0'
                      and $linenumbering.extension != '0'">
     <xsl:call-template name="number.rtf.lines">
      <xsl:with-param name="rtf">
       <xsl:apply-templates/>
      </xsl:with-param>
     </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
     <xsl:apply-templates/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>

  <xsl:choose>
   <xsl:when test="@class='monospaced'">
    <xsl:choose>
     <xsl:when test="$shade.verbatim != 0">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="monospace.verbatim.properties shade.verbatim.style">
       <xsl:if test="$keep.together != ''">
        <xsl:attribute name="keep-together.within-column"><xsl:value-of
         select="$keep.together"/></xsl:attribute>
       </xsl:if>
       <xsl:copy-of select="$content"/>
      </fo:block>
     </xsl:when>
     <xsl:otherwise>
      <fo:block id="{$id}"
                xsl:use-attribute-sets="monospace.verbatim.properties">
       <xsl:if test="$keep.together != ''">
        <xsl:attribute name="keep-together.within-column"><xsl:value-of
         select="$keep.together"/></xsl:attribute>
       </xsl:if>
       <xsl:copy-of select="$content"/>
      </fo:block>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:when>
   <xsl:otherwise>
    <xsl:choose>
     <xsl:when test="$shade.verbatim != 0">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="verbatim.properties shade.verbatim.style">
       <xsl:if test="$keep.together != ''">
        <xsl:attribute name="keep-together.within-column"><xsl:value-of
         select="$keep.together"/></xsl:attribute>
       </xsl:if>
       <xsl:copy-of select="$content"/>
      </fo:block>
     </xsl:when>
     <xsl:otherwise>
      <fo:block id="{$id}"
                xsl:use-attribute-sets="verbatim.properties">
       <xsl:if test="$keep.together != ''">
        <xsl:attribute name="keep-together.within-column"><xsl:value-of
         select="$keep.together"/></xsl:attribute>
       </xsl:if>
       <xsl:copy-of select="$content"/>
      </fo:block>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

</xsl:stylesheet>
