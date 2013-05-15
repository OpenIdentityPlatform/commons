package org.forgerock.json.resource.servlet;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@SuppressWarnings("javadoc")
class StringBuilderOutputStream extends ServletOutputStream
{

  private final StringBuilder output;

  StringBuilderOutputStream(StringBuilder output)
  {
    this.output = output;
  }

  @Override
  public void write(int b)
  {
    throw new NotImplementedException();
  }

  @Override
  public void write(byte[] b) throws IOException
  {
    output.append(new String(b));
  }

  @Override
  public void write(byte buf[], int offset, int len) throws IOException
  {
    if (!(len >= 0))
    {
      throw new IllegalArgumentException("len parameter must be positive");
    }
    output.append(new String(buf, offset, len));
  }

}
