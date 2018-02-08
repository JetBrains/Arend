package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class SimpleGlobalReferable implements GlobalReferable {
  private final Precedence myPrecedence;
  private final String myName;
  private final GlobalReferable myTypecheckable;

  public SimpleGlobalReferable(Precedence precedence, String name, GlobalReferable typecheckable) {
    myPrecedence = precedence;
    myName = name;
    myTypecheckable = typecheckable == null ? this : typecheckable;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public GlobalReferable getTypecheckable() {
    return myTypecheckable;
  }
}
