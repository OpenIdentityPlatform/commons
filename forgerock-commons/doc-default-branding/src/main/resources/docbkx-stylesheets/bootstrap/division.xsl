<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="db ng exsl exslt"
                version='1.0'>

<!-- ********************************************************************
     $Id: division.xsl 9366 2012-05-12 23:44:25Z bobstayton $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->


<xsl:template match="db:book">
  <xsl:call-template name="id.warning"/>

 <xsl:variable name="toc.params">
  <xsl:call-template name="find.path.params">
   <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
  </xsl:call-template>
 </xsl:variable>

 <xsl:call-template name="make.lots">
  <xsl:with-param name="toc.params" select="$toc.params"/>
  <xsl:with-param name="toc">
   <xsl:call-template name="division.toc">
    <xsl:with-param name="toc.title.p" select="contains($toc.params, 'title')"/>
   </xsl:call-template>
  </xsl:with-param>
 </xsl:call-template>

 <!-- Add bootstrap classes to contents to make it a wide, right-aligned column //-->
 <div id="doc-content" class="col-sm-9">
   <xsl:variable name="id" select="doc-content"/>
    <xsl:call-template name="book.titlepage"/>

    <xsl:apply-templates select="dedication" mode="dedication"/>
    <xsl:apply-templates select="acknowledgements" mode="acknowledgements"/>

   <xsl:apply-templates/>
  </div>
</xsl:template>

</xsl:stylesheet>

