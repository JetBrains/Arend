package org.arend.naming.reference;

import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetaReferable implements GlobalReferable, MetaRef {
  private final Precedence myPrecedence;
  private final String myName;
  private final MetaDefinition myDefinition;
  private final MetaResolver myResolver;
  public final String description;
  private final String myAliasName;
  private final Precedence myAliasPrecedence;
  public GlobalReferable underlyingReferable;

  public MetaReferable(Precedence precedence, String name, Precedence aliasPrec, String aliasName, String description, MetaDefinition definition, MetaResolver resolver) {
    myPrecedence = precedence;
    myName = name;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrec == null ? Precedence.DEFAULT : aliasPrec;
    this.description = description;
    myDefinition = definition;
    myResolver = resolver;
  }

  public MetaReferable(Precedence precedence, String name, String description, MetaDefinition definition, MetaResolver resolver) {
    this(precedence, name, null, null, description, definition, resolver);
  }

  @Nullable
  @Override
  public MetaDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public @Nullable MetaResolver getResolver() {
    return myResolver;
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

  @Override
  public @Nullable String getAliasName() {
    return myAliasName;
  }

  @Override
  public @NotNull Precedence getAliasPrecedence() {
    return myAliasPrecedence;
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
