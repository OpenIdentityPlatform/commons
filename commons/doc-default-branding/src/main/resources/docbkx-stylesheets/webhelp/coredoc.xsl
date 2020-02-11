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
 <xsl:import href="urn:docbkx:stylesheet/webhelp.xsl" />

 <xsl:output method="html" encoding="UTF-8" indent="no" />
 <xsl:preserve-space elements="d:programlisting d:screen"/>

 <!--
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
    <xsl:choose>
     <xsl:when test="@language='shell'">
      <pre class="brush: shell;"><xsl:value-of select="." /></pre>
     </xsl:when>
     <xsl:otherwise>
      <pre class="brush: plain;"><xsl:value-of select="." /></pre>
     </xsl:otherwise>
    </xsl:choose>
   </div>
  </xsl:template>

  <xsl:param name="make.clean.html" select="1" />
  <xsl:param name="docbook.css.link" select="0" />
  <xsl:param name="docbook.css.source" select="0" />
  <xsl:param name="custom.css.source">coredoc.css.xml</xsl:param>
  <xsl:param name="html.script">uses-jquery.js</xsl:param>
 -->

 <xsl:param name="admon.style">
  <xsl:value-of select="string('font-style: italic;')"></xsl:value-of>
 </xsl:param>
 <xsl:param name="admon.graphics" select="1" />

 <xsl:param name="suppress.footer.navigation" select="1" />

 <xsl:param name="generate.legalnotice.link" select="1" />
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

 <!--
   DOCS-206
   Override default XSL so that the DRAFT watermark appears correctly in Webhelp
-->
 <xsl:template name="head.content.style">
  <xsl:param name="node" select="."/>
  <style type="text/css"><xsl:text>
body { background-image: url('</xsl:text>
   <xsl:value-of select="$draft.watermark.image"/><xsl:text>');
       background-repeat: no-repeat;
       background-attachment: fixed;
       background-position: center center;
     }</xsl:text>
  </style>
 </xsl:template>

 <!-- HTML <head> section customizations -->
 <xsl:template name="user.head.content">
  <xsl:param name="title">
   <xsl:apply-templates select="." mode="object.title.markup.textonly"/>
  </xsl:param>
  <meta name="Section-title" content="{$title}"/>

  <!--  <xsl:message>
      webhelp.tree.cookie.id = <xsl:value-of select="$webhelp.tree.cookie.id"/> +++ <xsl:value-of select="count(//node())"/>
      $webhelp.indexer.language = <xsl:value-of select="$webhelp.indexer.language"/> +++ <xsl:value-of select="count(//node())"/>
  </xsl:message>-->
  <script type="text/javascript">
   //The id for tree cookie
   var treeCookieId = "<xsl:value-of select="$webhelp.tree.cookie.id"/>";
   var language = "<xsl:value-of select="$webhelp.indexer.language"/>";
   var w = new Object();
   //Localization
   txt_filesfound = '<xsl:call-template name="gentext.template">
   <xsl:with-param name="name" select="'txt_filesfound'"/>
   <xsl:with-param name="context" select="'webhelp'"/>
  </xsl:call-template>';
   txt_enter_at_least_1_char = "<xsl:call-template name="gentext.template">
   <xsl:with-param name="name" select="'txt_enter_at_least_1_char'"/>
   <xsl:with-param name="context" select="'webhelp'"/>
  </xsl:call-template>";
   txt_browser_not_supported = "<xsl:call-template name="gentext.template">
   <xsl:with-param name="name" select="'txt_browser_not_supported'"/>
   <xsl:with-param name="context" select="'webhelp'"/>
  </xsl:call-template>";
   txt_please_wait = "<xsl:call-template name="gentext.template">
   <xsl:with-param name="name" select="'txt_please_wait'"/>
   <xsl:with-param name="context" select="'webhelp'"/>
  </xsl:call-template>";
   txt_results_for = "<xsl:call-template name="gentext.template">
   <xsl:with-param name="name" select="'txt_results_for'"/>
   <xsl:with-param name="context" select="'webhelp'"/>
  </xsl:call-template>";
  </script>

  <!-- kasunbg: Order is important between the in-html-file css and the linked css files. Some css declarations in jquery-ui-1.8.2.custom.css are over-ridden.
       If that's a concern, just remove the additional css contents inside these default jquery css files. I thought of keeping them intact for easier maintenance! -->
  <link rel="shortcut icon" href="favicon.ico" type="image/x-icon"/>
  <link rel="stylesheet" type="text/css" href="{$webhelp.common.dir}css/positioning.css"/>
  <link rel="stylesheet" type="text/css" href="{$webhelp.common.dir}jquery/theme-redmond/jquery-ui-1.8.2.custom.css"/>
  <link rel="stylesheet" type="text/css" href="{$webhelp.common.dir}jquery/treeview/jquery.treeview.css"/>

  <style type="text/css">

   #noscript{
   font-weight:bold;
   background-color: #55AA55;
   font-weight: bold;
   height: 25spx;
   z-index: 3000;
   top:0px;
   width:100%;
   position: relative;
   border-bottom: solid 5px black;
   text-align:center;
   color: white;
   }

   input {
   margin-bottom: 5px;
   margin-top: 2px;
   }
   .folder {
   display: block;
   height: 22px;
   padding-left: 20px;
   background: transparent url(<xsl:value-of select="$webhelp.common.dir"/>jquery/treeview/images/folder.gif) 0 0px no-repeat;
   }
   span.contentsTab {
   padding-left: 20px;
   background: url(<xsl:value-of select="$webhelp.common.dir"/>images/toc-icon.png) no-repeat 0 center;
   }
   span.searchTab {
   padding-left: 20px;
   background: url(<xsl:value-of select="$webhelp.common.dir"/>images/search-icon.png) no-repeat 0 center;
   }

   /* Overide jquery treeview's defaults for ul. */
   .treeview ul {
   background-color: transparent;
   margin-top: 4px;
   }
   #webhelp-currentid {
   background-color: #D8D8D8 !important;
   }
   .treeview .hover { color: black; }
   .filetree li span a { text-decoration: none; font-size: 12px; color: #517291; }

   /* Override jquery-ui's default css customizations. These are supposed to take precedence over those.*/
   .ui-widget-content {
   border: 0px;
   background: none;
   color: none;
   }
   .ui-widget-header {
   color: #e9e8e9;
   border-left: 1px solid #e5e5e5;
   border-right: 1px solid #e5e5e5;
   border-bottom: 1px solid #bbc4c5;
   border-top: 4px solid #e5e5e5;
   border: medium none;
   background: #F4F4F4; /* old browsers */
   background: -moz-linear-gradient(top, #F4F4F4 0%, #E6E4E5 100%); /* firefox */
   background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#F4F4F4), color-stop(100%,#E6E4E5)); /* webkit */
   font-weight: none;
   }
   .ui-widget-header a { color: none; }
   .ui-state-default, .ui-widget-content .ui-state-default, .ui-widget-header .ui-state-default {
   border: none; background: none; font-weight: none; color: none; }
   .ui-state-default a, .ui-state-default a:link, .ui-state-default a:visited { color: black; text-decoration: none; }
   .ui-state-hover, .ui-widget-content .ui-state-hover, .ui-widget-header .ui-state-hover, .ui-state-focus, .ui-widget-content .ui-state-focus, .ui-widget-header .ui-state-focus { border: none; background: none; font-weight: none; color: none; }

   .ui-state-active, .ui-widget-content .ui-state-active, .ui-widget-header .ui-state-active { border: none; background: none; font-weight: none; color: none; }
   .ui-state-active a, .ui-state-active a:link, .ui-state-active a:visited {
   color: black; text-decoration: none;
   background: #C6C6C6; /* old browsers */
   background: -moz-linear-gradient(top, #C6C6C6 0%, #D8D8D8 100%); /* firefox */
   background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,#C6C6C6), color-stop(100%,#D8D8D8)); /* webkit */
   -webkit-border-radius:15px; -moz-border-radius:10px;
   border: 1px solid #f1f1f1;
   }
   .ui-corner-all { border-radius: 0 0 0 0; }

   .ui-tabs { padding: .2em;}
   .ui-tabs .ui-tabs-nav li { top: 0px; margin: -2px 0 1px; text-transform: uppercase; font-size: 10.5px;}
   .ui-tabs .ui-tabs-nav li a { padding: .25em 2em .25em 1em; margin: .5em; text-shadow: 0 1px 0 rgba(255,255,255,.5); }
   /**
   *	Basic Layout Theme
   *
   *	This theme uses the default layout class-names for all classes
   *	Add any 'custom class-names', from options: paneClass, resizerClass, togglerClass
   */

   .ui-layout-pane { /* all 'panes' */
   background: transparent;
   border: 1px solid #BBB;
   padding: 05x;
   overflow: auto;
   }

   .ui-layout-resizer { /* all 'resizer-bars' */
   background: #DDD;
   top:100px
   }

   .ui-layout-toggler { /* all 'toggler-buttons' */
   background: #AAA;
   }

  </style>
  <xsl:comment><xsl:text>[if IE]>
	&lt;link rel="stylesheet" type="text/css" href="../common/css/ie.css"/>
	&lt;![endif]</xsl:text></xsl:comment>

  <!--
       browserDetect is an Oxygen addition to warn the user if they're using chrome from the file system.
       This breaks the Oxygen search highlighting.
  -->
  <script type="text/javascript" src="{$webhelp.common.dir}browserDetect.js">
   <xsl:comment> </xsl:comment>
  </script>
  <script type="text/javascript" src="{$webhelp.common.dir}jquery/jquery-1.7.2.min.js">
   <xsl:comment> </xsl:comment>
  </script>
  <script type="text/javascript" src="{$webhelp.common.dir}jquery/jquery.ui.all.js">
   <xsl:comment> </xsl:comment>
  </script>
  <script type="text/javascript" src="{$webhelp.common.dir}jquery/jquery.cookie.js">
   <xsl:comment> </xsl:comment>
  </script>
  <script type="text/javascript" src="{$webhelp.common.dir}jquery/treeview/jquery.treeview.min.js">
   <xsl:comment> </xsl:comment>
  </script>
  <script type="text/javascript" src="{$webhelp.common.dir}jquery/layout/jquery.layout.js">
   <xsl:comment> </xsl:comment>
  </script>
  <xsl:if test="$webhelp.include.search.tab != '0'">
   <!--Scripts/css stylesheets for Search-->
   <!-- TODO: Why THREE files? There's absolutely no need for having separate files.
These should have been identified at the optimization phase! -->
   <script type="text/javascript" src="search/l10n.js">
    <xsl:comment/>
   </script>
   <script type="text/javascript" src="search/htmlFileInfoList.js">
    <xsl:comment> </xsl:comment>
   </script>
   <script type="text/javascript" src="search/nwSearchFnt.js">
    <xsl:comment> </xsl:comment>
   </script>

   <!--
      NOTE: Stemmer javascript files should be in format <language>_stemmer.js.
      For example, for English(en), source should be: "search/stemmers/en_stemmer.js"
      For country codes, see: http://www.uspto.gov/patft/help/helpctry.htm
   -->
   <!--<xsl:message><xsl:value-of select="concat('search/stemmers/',$webhelp.indexer.language,'_stemmer.js')"/></xsl:message>-->
   <script type="text/javascript" src="{concat('search/stemmers/',$webhelp.indexer.language,'_stemmer.js')}">
    <xsl:comment>//make this scalable to other languages as well.</xsl:comment>
   </script>

   <!--Index Files:
       Index is broken in to three equal sized(number of index items) files. This is to help parallel downloading
       of files to make it faster.
  TODO: Generate webhelp index for largest docbook document that can be find, and analyze the file sizes.
  IF the file size is still around ~50KB for a given file, we should consider merging these files together. again.
   -->
   <script type="text/javascript" src="search/index-1.js">
    <xsl:comment> </xsl:comment>
   </script>
   <script type="text/javascript" src="search/index-2.js">
    <xsl:comment> </xsl:comment>
   </script>
   <script type="text/javascript" src="search/index-3.js">
    <xsl:comment> </xsl:comment>
   </script>
   <!--End of index files -->
  </xsl:if>
  <xsl:call-template name="user.webhelp.head.content"/>
 </xsl:template>

</xsl:stylesheet>
