package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import javax.annotation.Nonnull;

public class GlobalReference implements GlobalReferable {
  private final String myName;
  private Concrete.Definition<Position> myDefinition;

  public GlobalReference(@Nonnull String name) {
    myName = name;
  }

  public Concrete.Definition<Position> getDefinition() {
    return myDefinition;
  }

  public void setDefinition(Concrete.Definition<Position> definition) {
    assert myDefinition == null;
    myDefinition = definition;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }
}
