package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TypedRedirectingReferable implements RedirectingReferable {
  private final Referable myOriginalReferable;
  private final ClassReferable myTypeClassReference;

  public TypedRedirectingReferable(Referable originalReferable, ClassReferable typeClassReference) {
    myOriginalReferable = originalReferable;
    myTypeClassReference = typeClassReference;
  }

  @NotNull
  @Override
  public Referable getUnderlyingReferable() {
    return myOriginalReferable.getUnderlyingReferable();
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getPrecedence() : Precedence.DEFAULT;
  }

  @NotNull
  @Override
  public Referable getOriginalReferable() {
    return myOriginalReferable instanceof RedirectingReferable ? ((RedirectingReferable) myOriginalReferable).getOriginalReferable() : myOriginalReferable;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myOriginalReferable.textRepresentation();
  }

  @Override
  public GlobalReferable getTypecheckable() {
    return myOriginalReferable instanceof GlobalReferable ? ((GlobalReferable) myOriginalReferable).getTypecheckable() : null;
  }

  @NotNull
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
