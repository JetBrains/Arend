package org.arend.naming.reference;

import org.arend.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TypedRedirectingReferable implements RedirectingReferable {
  private final Referable myOriginalReferable;
  private final ClassReferable myTypeClassReference;

  public TypedRedirectingReferable(Referable originalReferable, ClassReferable typeClassReference) {
    myOriginalReferable = originalReferable;
    myTypeClassReference = typeClassReference;
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getPrecedence() : Precedence.DEFAULT;
  }

  @Nonnull
  @Override
  public Referable getOriginalReferable() {
    return myOriginalReferable instanceof RedirectingReferable ? ((RedirectingReferable) myOriginalReferable).getOriginalReferable() : myOriginalReferable;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myOriginalReferable.textRepresentation();
  }

  @Override
  public GlobalReferable getTypecheckable() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getTypecheckable() : null;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getKind() : Kind.OTHER;
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myTypeClassReference;
  }
}
