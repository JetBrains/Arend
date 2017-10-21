package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class SourceIdReference implements GlobalReferable {
  public final SourceId sourceId;

  public SourceIdReference(@Nonnull SourceId sourceId) {
    this.sourceId = sourceId;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return sourceId.getModulePath().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SourceIdReference that = (SourceIdReference) o;

    return sourceId.equals(that.sourceId);
  }

  @Override
  public int hashCode() {
    return sourceId.hashCode();
  }
}
