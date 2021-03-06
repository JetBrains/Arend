package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.jetbrains.annotations.NotNull;

public class AliasReferable implements RedirectingReferable {
  private final GlobalReferable myReferable;

  public AliasReferable(GlobalReferable referable) {
    myReferable = referable;
  }

  @NotNull
  @Override
  public Referable getUnderlyingReferable() {
    return myReferable.getUnderlyingReferable();
  }

  @Override
  public @NotNull GlobalReferable getOriginalReferable() {
    return myReferable;
  }

  @Override
  public @NotNull Precedence getPrecedence() {
    return myReferable.getAliasPrecedence();
  }

  @Override
  public @NotNull String textRepresentation() {
    String name = myReferable.getAliasName();
    return name == null ? myReferable.textRepresentation() : name;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof RedirectingReferable && ((RedirectingReferable) obj).getOriginalReferable().equals(getOriginalReferable());
  }
}
