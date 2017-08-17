package com.jetbrains.jetpad.vclang.frontend.text;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

public class Position {
  public final SourceId module;
  public final int line;
  public final int column;

  public Position(SourceId module, int line, int column) {
    this.module = module;
    this.line = line;
    this.column = column + 1;
  }

  @Override
  public String toString() {
    return (module == null ? "" : module + ":") + line + ":" + column;
  }
}
