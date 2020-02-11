<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="db ng exsl exslt"
                version='1.0'>

 <!-- ********************************************************************
      $Id: toc.xsl 9297 2012-04-22 03:56:16Z bobstayton $
      ********************************************************************

      This file is part of the XSL DocBook Stylesheet distribution.
      See ../README or http://docbook.sf.net/release/xsl/current/ for
      copyright and other information.

      ******************************************************************** -->

 <!-- ==================================================================== -->

<!-- <xsl:template match="db:set/db:toc | db:book/db:toc | db:part/db:toc">
  <xsl:variable name="toc.params">
   <xsl:call-template name="find.path.params">
    <xsl:with-param name="node" select="parent::*"/>
    <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
   </xsl:call-template>
  </xsl:variable>

  &lt;!&ndash; Do not output the toc element if one is already generated
       by the use of $generate.toc parameter, or if
       generating a source toc is turned off &ndash;&gt;
  <xsl:if test="not(contains($toc.params, 'toc')) and
                ($process.source.toc != 0 or $process.empty.source.toc != 0)">
   <xsl:variable name="content">
    <xsl:choose>
     <xsl:when test="* and $process.source.toc != 0">
      <xsl:apply-templates />
     </xsl:when>
     <xsl:when test="count(*) = 0 and $process.empty.source.toc != 0">
      &lt;!&ndash; trick to switch context node to parent element &ndash;&gt;
      <xsl:for-each select="parent::*">
       <xsl:choose>
        <xsl:when test="self::set">
         <xsl:call-template name="set.toc">
          <xsl:with-param name="toc.title.p"
                          select="contains($toc.params, 'title')"/>
         </xsl:call-template>
        </xsl:when>
        <xsl:when test="self::book">
         <xsl:call-template name="division.toc">
          <xsl:with-param name="toc.title.p"
                          select="contains($toc.params, 'title')"/>
         </xsl:call-template>
        </xsl:when>
        <xsl:when test="self::part">
         <xsl:call-template name="division.toc">
          <xsl:with-param name="toc.title.p"
                          select="contains($toc.params, 'title')"/>
         </xsl:call-template>
        </xsl:when>
       </xsl:choose>
      </xsl:for-each>
     </xsl:when>
    </xsl:choose>
   </xsl:variable>

   <xsl:if test="string-length(normalize-space($content)) != 0">
    <xsl:copy-of select="$content"/>
   </xsl:if>
  </xsl:if>
 </xsl:template>

 <xsl:template match="db:chapter/db:toc | db:appendix/db:toc | db:preface/db:toc | db:article/db:toc">
  <xsl:variable name="toc.params">
   <xsl:call-template name="find.path.params">
    <xsl:with-param name="node" select="parent::*"/>
    <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
   </xsl:call-template>
  </xsl:variable>

  &lt;!&ndash; Do not output the toc element if one is already generated
       by the use of $generate.toc parameter, or if
       generating a source toc is turned off &ndash;&gt;
  <xsl:if test="not(contains($toc.params, 'toc')) and
                ($process.source.toc != 0 or $process.empty.source.toc != 0)">
   <xsl:choose>
    <xsl:when test="* and $process.source.toc != 0">
     <div>
      <xsl:apply-templates select="." mode="common.html.attributes"/>
      <xsl:call-template name="id.attribute"/>
      <xsl:apply-templates select="title"/>
      <dl>
       <xsl:apply-templates select="." mode="common.html.attributes"/>
       <xsl:apply-templates select="*[not(self::title)]"/>
      </dl>
     </div>
     <xsl:call-template name="component.toc.separator"/>
    </xsl:when>
    <xsl:when test="count(*) = 0 and $process.empty.source.toc != 0">
     &lt;!&ndash; trick to switch context node to section element &ndash;&gt;
     <xsl:for-each select="parent::*">
      <xsl:call-template name="component.toc">
       <xsl:with-param name="toc.title.p"
                       select="contains($toc.params, 'title')"/>
      </xsl:call-template>
     </xsl:for-each>
     <xsl:call-template name="component.toc.separator"/>
    </xsl:when>
   </xsl:choose>
  </xsl:if>
 </xsl:template>-->

<!-- <xsl:template match="db:section/db:toc
                    |db:sect1/db:toc
                    |db:sect2/db:toc
                    |db:sect3/db:toc
                    |db:sect4/db:toc
                    |db:sect5/db:toc">

  <xsl:variable name="toc.params">
   <xsl:call-template name="find.path.params">
    <xsl:with-param name="node" select="parent::*"/>
    <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
   </xsl:call-template>
  </xsl:variable>

  &lt;!&ndash; Do not output the toc element if one is already generated
       by the use of $generate.toc parameter, or if
       generating a source toc is turned off &ndash;&gt;
  <xsl:if test="not(contains($toc.params, 'toc')) and
                ($process.source.toc != 0 or $process.empty.source.toc != 0)">
   <xsl:choose>
    <xsl:when test="* and $process.source.toc != 0">
     <div>
      <xsl:apply-templates select="." mode="common.html.attributes"/>
      <xsl:call-template name="id.attribute"/>
      <xsl:apply-templates select="db:title"/>
      <dl>
       <xsl:apply-templates select="." mode="common.html.attributes"/>
       <xsl:apply-templates select="*[not(self::title)]"/>
      </dl>
     </div>
     <xsl:call-template name="section.toc.separator"/>
    </xsl:when>
    <xsl:when test="count(*) = 0 and $process.empty.source.toc != 0">
     &lt;!&ndash; trick to switch context node to section element &ndash;&gt;
     <xsl:for-each select="parent::*">
      <xsl:call-template name="section.toc">
       <xsl:with-param name="toc.title.p"
                       select="contains($toc.params, 'title')"/>
      </xsl:call-template>
     </xsl:for-each>
     <xsl:call-template name="section.toc.separator"/>
    </xsl:when>
   </xsl:choose>
  </xsl:if>
 </xsl:template>-->

 <!-- ==================================================================== -->

 <xsl:template match="db:tocpart|db:tocchap
                     |db:toclevel1|db:toclevel2|db:toclevel3|db:toclevel4|db:toclevel5">
  <xsl:variable name="sub-toc">
   <xsl:if test="db:tocchap|db:toclevel1|db:toclevel2|db:toclevel3|db:toclevel4|db:toclevel5">
    <xsl:choose>
     <xsl:when test="$toc.list.type = 'dl'">
      <dd>
       <xsl:apply-templates select="." mode="common.html.attributes"/>
       <xsl:element name="{$toc.list.type}">
        <xsl:apply-templates select="." mode="common.html.attributes"/>
        <xsl:apply-templates select="db:tocchap|db:toclevel1|db:toclevel2|
                                           db:toclevel3|db:toclevel4|db:toclevel5"/>
       </xsl:element>
      </dd>
     </xsl:when>
     <xsl:otherwise>
      <ul class="nav nav-stacked">

       <xsl:apply-templates select="db:tocchap|db:toclevel1|db:toclevel2|
                                         db:toclevel3|db:toclevel4|db:toclevel5"/>
      </ul>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:if>
  </xsl:variable>

  <xsl:apply-templates select="db:tocentry[position() != last()]"/>

  <xsl:choose>
   <xsl:when test="$toc.list.type = 'dl'">
    <dt>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:apply-templates select="db:tocentry[position() = last()]"/>
    </dt>
    <xsl:copy-of select="$sub-toc"/>
   </xsl:when>
   <xsl:otherwise>
    <li>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:apply-templates select="db:tocentry[position() = last()]"/>
     <xsl:copy-of select="$sub-toc"/>
    </li>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

<!-- <xsl:template match="db:tocentry|db:tocdiv|db:lotentry|db:tocfront|db:tocback">
  <xsl:choose>
   <xsl:when test="$toc.list.type = 'dl'">
    <dt>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:call-template name="tocentry-content"/>
    </dt>
   </xsl:when>
   <xsl:otherwise>
    <li>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:call-template name="tocentry-content"/>
    </li>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="db:tocentry[position() = last()]" priority="2">
  <xsl:call-template name="tocentry-content"/>
 </xsl:template>

 <xsl:template name="tocentry-content">
  <xsl:variable name="targets" select="key('id',@linkend)"/>
  <xsl:variable name="target" select="$targets[1]"/>

  <xsl:choose>
   <xsl:when test="@linkend">
    <xsl:call-template name="check.id.unique">
     <xsl:with-param name="linkend" select="@linkend"/>
    </xsl:call-template>
    <a>
     <xsl:attribute name="href">
      <xsl:call-template name="href.target">
       <xsl:with-param name="object" select="$target"/>
      </xsl:call-template>
     </xsl:attribute>
     <xsl:apply-templates/>
    </a>
   </xsl:when>
   <xsl:otherwise>
    <xsl:apply-templates/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="db:toc/db:title">
  <div>
   <xsl:apply-templates select="." mode="common.html.attributes"/>
   <xsl:apply-templates/>
  </div>
 </xsl:template>

 <xsl:template match="db:toc/db:subtitle">
  <div>
   <xsl:apply-templates select="." mode="common.html.attributes"/>
   <xsl:apply-templates/>
  </div>
 </xsl:template>

 <xsl:template match="db:toc/db:titleabbrev">
 </xsl:template>

 &lt;!&ndash; ==================================================================== &ndash;&gt;

 &lt;!&ndash; A lot element must have content, because there is no attribute
      to select what kind of list should be generated &ndash;&gt;
 <xsl:template match="db:book/db:lot | db:part/db:lot">
  &lt;!&ndash; Don't generate a page sequence unless there is content &ndash;&gt;
  <xsl:variable name="content">
   <xsl:choose>
    <xsl:when test="* and $process.source.toc != 0">
     <div>
      <xsl:apply-templates select="." mode="common.html.attributes"/>
      <xsl:apply-templates />
     </div>
    </xsl:when>
    <xsl:when test="not(child::*) and $process.empty.source.toc != 0">
     <xsl:call-template name="process.empty.lot"/>
    </xsl:when>
   </xsl:choose>
  </xsl:variable>

  <xsl:if test="string-length(normalize-space($content)) != 0">
   <xsl:copy-of select="$content"/>
  </xsl:if>
 </xsl:template>

 <xsl:template match="db:chapter/db:lot | db:appendix/db:lot | db:preface/db:lot | db:article/db:lot">
  <xsl:choose>
   <xsl:when test="* and $process.source.toc != 0">
    <div>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:apply-templates />
    </div>
    <xsl:call-template name="component.toc.separator"/>
   </xsl:when>
   <xsl:when test="not(child::*) and $process.empty.source.toc != 0">
    <xsl:call-template name="process.empty.lot"/>
   </xsl:when>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="db:section/db:lot
                    |db:sect1/db:lot
                    |db:sect2/db:lot
                    |db:sect3/db:lot
                    |db:sect4/db:lot
                    |db:sect5/db:lot">
  <xsl:choose>
   <xsl:when test="* and $process.source.toc != 0">
    <div>
     <xsl:apply-templates select="." mode="common.html.attributes"/>
     <xsl:apply-templates/>
    </div>
    <xsl:call-template name="section.toc.separator"/>
   </xsl:when>
   <xsl:when test="not(child::*) and $process.empty.source.toc != 0">
    <xsl:call-template name="process.empty.lot"/>
   </xsl:when>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="process.empty.lot">
  &lt;!&ndash; An empty lot element does not provide any information to indicate
       what should be included in it.  You can customize this
       template to generate a lot based on @role or something &ndash;&gt;
  <xsl:message>
   <xsl:text>Warning: don't know what to generate for </xsl:text>
   <xsl:text>lot that has no children.</xsl:text>
  </xsl:message>
 </xsl:template>

 <xsl:template match="db:lot/db:title">
  <div>
   <xsl:apply-templates select="." mode="common.html.attributes"/>
   <xsl:apply-templates/>
  </div>
 </xsl:template>

 <xsl:template match="db:lot/db:subtitle">
  <div>
   <xsl:apply-templates select="." mode="common.html.attributes"/>
   <xsl:apply-templates/>
  </div>
 </xsl:template>

 <xsl:template match="db:lot/db:titleabbrev">
 </xsl:template>-->

</xsl:stylesheet>
