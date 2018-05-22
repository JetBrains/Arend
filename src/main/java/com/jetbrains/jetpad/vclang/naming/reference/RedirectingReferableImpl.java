package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedirectingReferableImpl implements RedirectingReferable {
  private final Referable myOriginalReferable;
  private final Precedence myPrecedence;
  private final String myName;

  public RedirectingReferableImpl(Referable originalReferable, Precedence precedence, String name) {
    myOriginalReferable = originalReferable;
    myPrecedence = precedence;
    myName = name;
  }

  @Nonnull
  @Override
  public Referable getOriginalReferable() {
    return myOriginalReferable;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence != null ? myPrecedence : myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getPrecedence() : Precedence.DEFAULT;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public GlobalReferable getTypecheckable() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getTypecheckable() : null;
  }

  @Override
  public boolean isTypecheckable() {
    return myOriginalReferable instanceof GlobalReferable && ((GlobalReferable) myOriginalReferable).isTypecheckable();
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myOriginalReferable instanceof TypedReferable ? ((TypedReferable) myOriginalReferable).getTypeClassReference() : null;
  }
}
