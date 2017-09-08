package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class GlobalReference implements GlobalReferable, SourceInfo {
  private final String myName;
  private Concrete.ReferableDefinition myDefinition;
  private final Precedence myPrecedence;

  public GlobalReference(@Nonnull String name, Precedence precedence) {
    myName = name;
    myPrecedence = precedence;
  }

  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  public Concrete.ReferableDefinition getDefinition() {
    return myDefinition;
  }

  public void setDefinition(Concrete.ReferableDefinition definition) {
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
    Object data = myDefinition.getData();
    return data instanceof SourceInfo ? ((SourceInfo) data).moduleTextRepresentation() : null;
  }

  @Override
  public String positionTextRepresentation() {
    Object data = myDefinition.getData();
    return data instanceof SourceInfo ? ((SourceInfo) data).positionTextRepresentation() : null;
  }
}
