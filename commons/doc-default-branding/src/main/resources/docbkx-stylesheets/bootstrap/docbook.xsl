<?xml version='1.0'?>
 <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                 xmlns:ng="http://docbook.org/docbook-ng"
                 xmlns:db="http://docbook.org/ns/docbook"
                 xmlns:exsl="http://exslt.org/common"
                 xmlns:exslt="http://exslt.org/common"
                 exclude-result-prefixes="db ng exsl exslt"
                 version='1.0'>


<!-- ********************************************************************
     $Id: docbook.xsl 9605 2012-09-18 10:48:54Z tom_schr $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     copyright and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:template match="*" mode="process.root">
  <xsl:variable name="doc" select="self::*"/>

  <xsl:call-template name="user.preroot"/>
  <xsl:call-template name="root.messages"/>

  <html>
    <xsl:call-template name="root.attributes"/>
    <head>
      <xsl:call-template name="system.head.content">
        <xsl:with-param name="node" select="$doc"/>
      </xsl:call-template>
      <xsl:call-template name="head.content">
        <xsl:with-param name="node" select="$doc"/>
      </xsl:call-template>
      <xsl:call-template name="user.head.content">
        <xsl:with-param name="node" select="$doc"/>
      </xsl:call-template>
    </head>
    <body>
     <xsl:call-template name="body.attributes"/>
     <!-- Add bootstrap page elements //-->
     <!-- Add bootstrap header nav bar //-->
      <div class="navbar navbar-inverse navbar-fixed-top">
       <nav class="container-fluid">
        <div class="navbar-header">
         <a href="index.html" class="navbar-brand">
          <img src='includes/logos/FR_logo_horiz_FC_rev.png'
               alt="ForgeRock Documentation"/>
         </a>
        </div>
        <ul id="pdf-link">
         <!-- If PDFs are built, a link to it will be inserted here as a post-build step. -->
        </ul>
       </nav>
      </div>
     <!-- Add bootstrap full width banner, for doc title //-->
     <div class="jumbotron">
      <div class="container-fluid">
       <h1>
        <xsl:value-of select="ancestor-or-self::db:book/db:info/db:title"/>
        <span> </span>
        <small><xsl:value-of select="ancestor-or-self::db:book/db:info/db:subtitle"/></small></h1>

       <p><xsl:value-of select="ancestor-or-self::db:book/db:info/db:abstract/db:para"/></p>
      </div>
     </div>
     <!-- Add container for background image //-->
      <div class="left-shape-content"></div>
     <!-- Add fluid container for full width main content section //-->
      <div class="container-fluid">
       <div class="row">
        <xsl:call-template name="user.header.content">
         <xsl:with-param name="node" select="$doc"/>
       </xsl:call-template>
       <xsl:apply-templates select="."/>
       <xsl:call-template name="user.footer.content">
         <xsl:with-param name="node" select="$doc"/>
       </xsl:call-template>
        <!-- Add container for back-to-top button //-->
        <a href="#" class="back-to-top hidden-xs">
         <button type="button" class="btn btn-primary btn-default">
          <span class="glyphicon glyphicon-chevron-up"></span></button>
        </a>
       </div>
      </div>
     <!-- *** Add bootstrap config popup - for future use ***
     <div class="modal fade" id="docConfig" tabindex="-1" role="dialog"
          aria-labelledby="exampleModalLabel" aria-hidden="true">
      <div class="modal-dialog">
       <div class="modal-content">
        <div class="modal-header">
         <button type="button" class="close" data-dismiss="modal"
                 aria-label="Close"><span aria-hidden="true">X</span></button>
         <h4 class="modal-title" id="docCustomization">Document
          Customization</h4>
        </div>
        <div class="modal-body">
         <form>
          <div class="form-group">
           <label for="exampleUrl" class="control-label">OpenAM
            Deployment URL:</label>
           <input type="text" class="form-control" id="exampleUrl"
                  value="https://openam.example.com:8443" />
          </div>
          <div class="form-group">
           <label for="exampleAdmin" class="control-label">OpenAM
            Admin User ID:</label>
           <input type="text" class="form-control" id="exampleAdmin"
                  value="amadmin" />
          </div>
          <div class="form-group">
           <label for="exampleSsoCookieName" class="control-label">SSO
            Cookie Name:</label>
           <input type="text" class="form-control" id="exampleSsoCookieName"
                  value="iPlanetDirectoryPro" />
          </div>
         </form>
        </div>
        <div class="modal-footer">
         <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
         <button type="button" class="btn btn-primary"
                 id="applyDocUpdate">Update
          Config</button>
        </div>
       </div>
      </div>
     </div> -->
     <!-- Add bootstrap footer bar //-->

        <div class="footer">
         <div class="container-fluid">
          <div class="footer-left"><span class="footer-item">Copyright ©
           <xsl:value-of select="db:info/db:copyright/db:year"/>&#160;
           <xsl:value-of select="db:info/db:copyright/db:holder"/></span></div>
          <div class="footer-right"><a target="_blank"
                                       class="footer-item snap-left"
                                       href="legalnotice.html"><i
           class="glyphicon glyphicon-briefcase"></i> Legal Notice</a> <a
           target="_blank" class="footer-item snap-left">
           <xsl:attribute name="href">
                <xsl:value-of select="'https://bugster.forgerock.org/jira/secure/CreateIssueDetails!init.jspa?pid=10290&amp;issuetype=1&amp;summary=Feedback on: '"/>
                <xsl:apply-templates select="/*[1]" mode="title.markup"/>
            </xsl:attribute>
           <i class="glyphicon glyphicon-ok-sign"></i> Feedback</a>
           <a target="_blank" class="footer-item snap-left" href="#" data-toggle="modal" data-target="#myModal"><i class="glyphicon glyphicon-info-sign"></i> About</a> </div>
         </div>
        </div>
    </body>
  </html>
  
  <!-- Generate any css files only once, not once per chunk -->
  <xsl:call-template name="generate.css.files"/>
</xsl:template>

</xsl:stylesheet>
