<?xml version='1.0' encoding='utf-8'?>
<#--
 baseName:        base name for document source, such as index
 basePath:        base of absolute path to target data file
 docNames:        list of document names such as reference and admin-guide
 extension:       output file extension such as html, pdf, or xhtml
 format:          output format such as xhtml5 or epub
 isChunked:       whether the output format is chunked HTML
 name():          wrapper to call NameUtils.renameDoc()
 projectName:     project name such as OpenAM
 projectVersion:  project version such as 3.1.0
 type:            output file type such as html, pdf, or xhtml
-->
<#if format != "epub">
<!DOCTYPE targetset [
<!-- targetdatabase.dtd -->
<!-- A DTD for managing cross reference target information -->

<!ELEMENT targetset (targetsetinfo?, sitemap*, document*) >

<!ELEMENT targetsetinfo ANY >

<!ELEMENT sitemap (dir) >

<!ELEMENT dir ((dir|document)*) >
<!ATTLIST dir
        name      CDATA   #REQUIRED
>

<!ELEMENT document (div*) >
<!ATTLIST document
        targetdoc CDATA   #REQUIRED
        uri       CDATA   #IMPLIED
        baseuri   CDATA   #IMPLIED
        href      CDATA   #IMPLIED
        dir       CDATA   #IMPLIED
>

<!ELEMENT div (ttl?, objttl?, xreftext?, (div|obj)*)>
<!ATTLIST div
        targetptr  CDATA   #IMPLIED
        element   CDATA   #IMPLIED
        name      CDATA   #IMPLIED
        number    CDATA   #IMPLIED
        href      CDATA   #IMPLIED
        lang      CDATA   #IMPLIED
        page      CDATA   #IMPLIED
>


<!ELEMENT ttl ANY >
<!ELEMENT objttl ANY >
<!ELEMENT xreftext ANY >

<!ELEMENT obj (ttl?, objttl?, xreftext?)>
<!ATTLIST obj
        targetptr  CDATA   #IMPLIED
        element   CDATA   #IMPLIED
        name      CDATA   #IMPLIED
        number    CDATA   #IMPLIED
        href      CDATA   #IMPLIED
        lang      CDATA   #IMPLIED
        page      CDATA   #IMPLIED
>

<#list docNames as doc>
<!ENTITY ${doc} SYSTEM '${basePath}/docbkx/${format}/${doc}/${baseName}.${type}.target.db'>
</#list>
]>
</#if>
<targetset>
 <sitemap>
  <dir name='doc'>
  <#if format == "epub">
   <#list docNames as doc>
   <document targetdoc='${doc}' baseuri='${name(projectName, doc, projectVersion, extension)}'>
    <xi:include
      href='${basePath}/docbkx/${format}/${doc}/${baseName}.${type}.target.db'
      xmlns:xi='http://www.w3.org/2001/XInclude'
    />
   </document>
   </#list>
  </#if>
  <#if format == "html" && isChunked>
   <#list docNames as doc>
   <dir name='${doc}'>
    <dir name='${baseName}'>
     <document targetdoc='${doc}' baseuri='../../${doc}/${baseName}/'>
      &${doc};
     </document>
    </dir>
   </dir>
   </#list>
  </#if>
  <#if format == "html" && !isChunked || format == "xhtml" || format == "bootstrap">
   <#list docNames as doc>
   <document targetdoc='${doc}' baseuri='../${doc}/${baseName}.${extension}'>
    &${doc};
   </document>
   </#list>
  </#if>
  <#if format == "pdf" || format == "rtf">
   <#list docNames as doc>
   <document targetdoc='${doc}' baseuri='${name(projectName, doc, projectVersion, extension)}'>
    &${doc};
   </document>
   </#list>
  </#if>
  <#if format == "webhelp">
   <#list docNames as doc>
   <document targetdoc='${doc}' baseuri='../${doc}/'>
    &${doc};
   </document>
   </#list>
  </#if>
  </dir>
 </sitemap>
</targetset>
