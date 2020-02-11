<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="db ng exsl exslt"
                version='1.0'>

<!-- ********************************************************************
     $Id: admon.xsl 9728 2013-03-08 00:16:41Z bobstayton $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<xsl:template name="nongraphical.admonition">
  <div>
    <xsl:call-template name="common.html.attributes">
      <xsl:with-param name="inherit" select="1"/>
    </xsl:call-template>
    <xsl:call-template name="id.attribute"/>
    <xsl:if test="$admon.style != '' and $make.clean.html = 0">
      <xsl:attribute name="style">
        <xsl:value-of select="$admon.style"/>
      </xsl:attribute>
    </xsl:if>
   <xsl:choose>
   <!-- Add panel class to admonitions, and panel type depending on the admonition type //-->
   <xsl:when test="local-name(.)='note'"><xsl:attribute name="class">panel panel-info</xsl:attribute></xsl:when>
   <xsl:when test="local-name(.)='warning'"><xsl:attribute name="class">panel panel-warning</xsl:attribute></xsl:when>
   <xsl:when test="local-name(.)='caution'"><xsl:attribute name="class">panel panel-danger</xsl:attribute></xsl:when>
   <xsl:when test="local-name(.)='tip'"><xsl:attribute name="class">panel panel-success</xsl:attribute></xsl:when>
   <xsl:when test="local-name(.)='important'"><xsl:attribute name="class">panel panel-primary</xsl:attribute></xsl:when>
   <xsl:otherwise><xsl:attribute name="class">panel panel-primary</xsl:attribute></xsl:otherwise>
  </xsl:choose>
    <xsl:if test="$admon.textlabel != 0 or db:title or db:info/db:title">
     <div class="panel-heading">
     <h3 class="panel-title">
        <xsl:call-template name="anchor"/>
        <xsl:apply-templates select="." mode="object.title.markup"/>
     </h3>
     </div>
    </xsl:if>
   <div class="panel-body">
    <xsl:apply-templates/>
   </div>
  </div>
</xsl:template>

</xsl:stylesheet>
