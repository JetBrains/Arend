package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;

public class RedirectingReferableImpl implements RedirectingReferable {
  private final Referable myOriginalReferable;
  private final Precedence myPrecedence;
  private final Referable myNewReferable;

  public RedirectingReferableImpl(Referable originalReferable, Precedence precedence, Referable newRef) {
    myOriginalReferable = originalReferable;
    myPrecedence = precedence;
    myNewReferable = newRef;
  }

  @Nonnull
  @Override
  public Referable getOriginalReferable() {
    return myOriginalReferable;
  }

  @Nonnull
  @Override
  public Referable getNewReferable() {
    return myNewReferable;
  }

  @Override
  public boolean isModule() {
    return myOriginalReferable instanceof GlobalReferable && ((GlobalReferable) myOriginalReferable).isModule();
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence != null ? myPrecedence : myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getPrecedence() : Precedence.DEFAULT;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myNewReferable.textRepresentation();
  }
}
