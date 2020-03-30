package org.arend.naming.reference;

import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetaReferable implements GlobalReferable, MetaRef {
  private final Precedence myPrecedence;
  private final String myName;
  private final MetaDefinition myDefinition;
  public final String description;
  public GlobalReferable underlyingReferable;

  public MetaReferable(Precedence precedence, String name, String description, MetaDefinition definition) {
    myPrecedence = precedence;
    myName = name;
    this.description = description;
    myDefinition = definition;
  }

  @Nullable
  @Override
  public MetaDefinition getDefinition() {
    return myDefinition;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName;
  }

  @NotNull
  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public @NotNull GlobalReferable getUnderlyingReferable() {
    return underlyingReferable == null ? this : underlyingReferable;
  }
}
