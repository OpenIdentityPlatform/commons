<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ng="http://docbook.org/docbook-ng"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="db ng exsl exslt"
                version='1.0'>

<!-- ********************************************************************
     $Id: lists.xsl 9307 2012-04-28 03:55:07Z bobstayton $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->


<!-- ==================================================================== -->

<xsl:template match="db:procedure">
  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Preserve order of PIs and comments -->
  <xsl:variable name="preamble"
        select="*[not(self::db:step
                  or self::db:title
                  or self::db:titleabbrev)]
                |comment()[not(preceding-sibling::db:step)]
                |processing-instruction()[not(preceding-sibling::db:step)]"/>

  <div>
    <xsl:call-template name="common.html.attributes"/>
    <xsl:call-template name="id.attribute">

      <xsl:with-param name="conditional">
        <xsl:choose>
          <xsl:when test="db:title">0</xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:call-template name="anchor">
      <xsl:with-param name="conditional">
        <xsl:choose>
          <xsl:when test="title">0</xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>
    </xsl:call-template>
    <div class="procedure-inner">
    <xsl:if test="(db:title or db:info/db:title) and $placement = 'before'">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>

    <xsl:apply-templates select="$preamble"/>

    <xsl:choose>
      <xsl:when test="count(db:step) = 1">
        <ul>
          <xsl:call-template name="generate.class.attribute"/>
          <xsl:apply-templates 
            select="db:step
                    |comment()[preceding-sibling::db:step]
                    |processing-instruction()[preceding-sibling::db:step]"/>
        </ul>
      </xsl:when>
      <xsl:otherwise>
        <ol>
          <xsl:call-template name="generate.class.attribute"/>
          <xsl:attribute name="type">
            <xsl:value-of select="substring($procedure.step.numeration.formats,1,1)"/>
          </xsl:attribute>
          <xsl:apply-templates 
            select="db:step
                    |comment()[preceding-sibling::db:step]
                    |processing-instruction()[preceding-sibling::db:step]"/>
        </ol>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="(db:title or db:info/db:title) and $placement != 'before'">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>
      </div>
  </div>
</xsl:template>
</xsl:stylesheet>
