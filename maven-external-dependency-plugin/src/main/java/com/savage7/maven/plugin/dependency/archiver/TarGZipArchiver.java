package com.savage7.maven.plugin.dependency.archiver;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;

public class TarGZipArchiver extends TarArchiver
{
    public TarGZipArchiver() throws ArchiverException
    {
        this.setupCompressionMethod();
    }

    private void setupCompressionMethod() throws ArchiverException
    {
        TarCompressionMethod compression = new TarCompressionMethod();
        compression.setValue("gzip");
        this.setCompression(compression);
    }

}