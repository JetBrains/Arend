package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import javax.annotation.Nonnull;

public class GlobalReference implements GlobalReferable, SourceInfo {
  private final String myName;
  private Concrete.ReferableDefinition<Position> myDefinition;

  public GlobalReference(@Nonnull String name) {
    myName = name;
  }

  public Concrete.ReferableDefinition<Position> getDefinition() {
    return myDefinition;
  }

  public void setDefinition(Concrete.ReferableDefinition<Position> definition) {
    assert myDefinition == null;
    myDefinition = definition;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public String moduleTextRepresentation() {
    //noinspection ConstantConditions
    return myDefinition.getData().moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    //noinspection ConstantConditions
    return myDefinition.getData().positionTextRepresentation();
  }
}
