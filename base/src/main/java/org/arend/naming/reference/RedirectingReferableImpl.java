package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.abs.Abstract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedirectingReferableImpl implements RedirectingReferable {
  private final Referable myOriginalReferable;
  private final Precedence myPrecedence;
  private final String myName;

  public RedirectingReferableImpl(Referable originalReferable, Precedence precedence, String name) {
    myOriginalReferable = originalReferable;
    myPrecedence = precedence;
    myName = name;
  }

  @NotNull
  @Override
  public Referable getUnderlyingReferable() {
    return myOriginalReferable.getUnderlyingReferable();
  }

  @NotNull
  @Override
  public Referable getOriginalReferable() {
    return myOriginalReferable;
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence != null ? myPrecedence : myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getPrecedence() : Precedence.DEFAULT;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @Override
  public GlobalReferable getTypecheckable() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getTypecheckable() : null;
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myOriginalReferable instanceof TypedReferable ? ((TypedReferable) myOriginalReferable).getTypeClassReference() : null;
  }

  @Nullable
  @Override
  public Abstract.Expression getTypeOf() {
    return myOriginalReferable instanceof TypedReferable ? ((TypedReferable) myOriginalReferable).getTypeOf() : null;
  }
}
