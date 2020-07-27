package org.arend.error;

import org.arend.ext.error.SourceInfo;

public class SourcePosition implements SourceInfo {
  public final String sourceName;
  public final int line;
  public final int column;
  public final int length;

  public SourcePosition(String sourceName, int line, int column, int length) {
    this.sourceName = sourceName;
    this.line = line;
    this.column = column;
    this.length = length;
  }

  @Override
  public String moduleTextRepresentation() {
    return sourceName;
  }

  @Override
  public String positionTextRepresentation() {
    return line + ":" + column;
  }
}
