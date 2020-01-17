package org.arend.error;

import org.arend.ext.error.SourceInfo;

public class SourcePosition implements SourceInfo {
  public final String sourceName;
  public final int line;
  public final int column;

  public SourcePosition(String sourceName, int line, int column) {
    this.sourceName = sourceName;
    this.line = line;
    this.column = column;
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
