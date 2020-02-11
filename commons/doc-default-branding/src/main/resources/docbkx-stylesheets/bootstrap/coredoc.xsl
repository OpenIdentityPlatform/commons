<?xml version="1.0" encoding="UTF-8"?>
<!--
  ! This work is licensed under the Creative Commons
  ! Attribution-NonCommercial-NoDerivs 3.0 Unported License.
  ! To view a copy of this license, visit
  ! http://creativecommons.org/licenses/by-nc-nd/3.0/
  ! or send a letter to Creative Commons, 444 Castro Street,
  ! Suite 900, Mountain View, California, 94041, USA.
  !
  ! You can also obtain a copy of the license at
  ! legal/CC-BY-NC-ND.txt.
  ! See the License for the specific language governing permissions
  ! and limitations under the License.
  !
  ! If applicable, add the following below this CCPL HEADER, with the fields
  ! enclosed by brackets "[]" replaced with your own identifying information:
  !      Portions Copyright [yyyy] [name of copyright owner]
  !
  !      Copyright 2011-2015 ForgeRock AS.
  !
-->

<!DOCTYPE xsl:stylesheet [
 <!ENTITY css SYSTEM "styles.css">
 <!ENTITY js SYSTEM "scripts.js">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="db ng exsl exslt"
                version='1.0'>
    <xsl:import href="urn:docbkx:stylesheet" />
    <xsl:include href="admon.xsl" />
    <xsl:include href="autotoc.xsl" />
    <xsl:include href="booktitlepage.xsl" />
    <xsl:include href="toc.xsl" />
    <xsl:include href="division.xsl" />
    <xsl:include href="docbook.xsl" />
    <xsl:include href="graphics.xsl" />
    <xsl:include href="table.xsl" />
    <xsl:include href="lists.xsl" />
    <xsl:include href="titlepage.templates.xsl" />


<xsl:output method="html" encoding="UTF-8" indent="no" />

<!--<xsl:variable name="target.database"
              select="index.html.target.db"/> //-->

 <xsl:preserve-space
 elements="db:computeroutput db:programlisting db:screen db:userinput"/>

 <xsl:template match="db:programlisting">
  <xsl:choose>
   <xsl:when test="@language='aci'">
    <pre class="codelisting prettyprint "><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='csv'">
    <pre
     class="codelisting prettyprint"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='html'">
    <pre class="codelisting prettyprint linenums lang-html"><xsl:value-of
     select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='http'">
    <pre class="codelisting prettyprint"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ini'">
    <pre class="codelisting prettyprint"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='java'">
    <pre class="codelisting prettyprint linenums lang-java"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='javascript'">
    <pre class="codelisting prettyprint linenums lang-js"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='ldif'">
    <pre class="codelisting prettyprint"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='shell'">
    <pre class="codelisting prettyprint lang-sh"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='xml'">
    <pre class="codelisting prettyprint lang-xml"><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:when test="@language='none'">
    <pre><xsl:value-of select="." /></pre>
   </xsl:when>
   <xsl:otherwise>
    <pre class="codelisting prettyprint"><xsl:value-of select="." /></pre>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="db:keycap">
  <kbd><xsl:value-of select="." /></kbd>
 </xsl:template>

 <xsl:template match="db:screen">
     <pre class="cmdline prettyprint">
      <xsl:apply-templates mode="screen"/>
     </pre>
 </xsl:template>

 <xsl:template match="*" mode="screen"><xsl:value-of select="."/></xsl:template>

 <xsl:template match="db:replaceable" mode="screen">
  <em><strong><xsl:apply-templates mode="screen"/></strong></em>
 </xsl:template>

 <xsl:template match="db:userinput" mode="screen">
  <strong><xsl:apply-templates mode="screen"/></strong>
 </xsl:template>

 <xsl:template match="db:computeroutput" mode="screen">
  <em><xsl:apply-templates mode="screen"/></em>
 </xsl:template>

 <xsl:param name="toc.list.type">ul</xsl:param>

 <xsl:param name="html.script">
  http://code.jquery.com/jquery-1.11.2.min.js
  http://code.jquery.com/ui/1.11.4/jquery-ui.min.js
  http://maxcdn.bootstrapcdn.com/bootstrap/3.3.0/js/bootstrap.min.js
 </xsl:param>

 <xsl:param name="make.clean.html" select="1" />
 <xsl:param name="docbook.css.link" select="0" />
 <xsl:param name="docbook.css.source" select="0" />
 <xsl:param name="custom.css.source" select="0" />
 <xsl:param name="generate.css.header" select="1" />

 <xsl:param name="admon.style">
  <xsl:value-of select="string('font-style: italic;')"></xsl:value-of>
 </xsl:param>
 <xsl:param name="default.table.frame">none</xsl:param>
 <xsl:param name="default.table.rules">none</xsl:param>
 <xsl:param name="table.cell.border.thickness">0pt</xsl:param>

 <xsl:param name="generate.legalnotice.link" select="1" />
 <xsl:param name="root.filename">index</xsl:param>
 <xsl:param name="use.id.as.filename" select="1" />
 <xsl:param name="make.graphic.viewport" select="0" />
 <xsl:param name="ignore.image.scaling" select="1" />
 <xsl:param name="html.longdesc" select="1" />
 <xsl:param name="html.longdesc.link" select="0" />


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
  reference nop
  sect1     nop
  sect2     nop
  sect3     nop
  sect4     nop
  sect5     nop
  section   nop
  set       nop
 </xsl:param>
 <xsl:param name="generate.section.toc.level" select="1" />
 <xsl:param name="toc.section.depth" select="3" />
 <xsl:param name="toc.max.depth" select="4" />
 <xsl:param name="section.autolabel" select="0" />
 <xsl:param name="section.autolabel.max.depth" select="3" />

 <xsl:param name="generate.meta.abstract" select="1" />
 <xsl:param name="use.extensions" select="1" />
 <xsl:param name="graphicsize.extension" select="0"/>
 <xsl:param name="generate.id.attributes" select="1" />

 <xsl:template name="system.head.content">
  <style type="text/css">
   &css;
  </style>
  <meta name="viewport">
   <xsl:attribute name="content">
    <xsl:text>width=device-width, initial-scale=1</xsl:text>
   </xsl:attribute>
  </meta>
  <xsl:if test="($draft.mode = 'yes')">
   <meta name="robots">
    <xsl:attribute name="content">
     <xsl:text>noindex,nofollow</xsl:text>
    </xsl:attribute>
   </meta>
  </xsl:if>
 </xsl:template>

 <xsl:template name="user.head.content">
    <script type="text/javascript" >
     &js;
    </script>
  </xsl:template>


 </xsl:stylesheet>
