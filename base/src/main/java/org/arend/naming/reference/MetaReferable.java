package org.arend.naming.reference;

import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.module.ModuleLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetaReferable implements LocatedReferable, MetaRef {
  private final Precedence myPrecedence;
  private final String myName;
  private MetaDefinition myDefinition;
  private final ModuleLocation myLocation;
  private final MetaResolver myResolver;
  public final String description;
  private final String myAliasName;
  private final Precedence myAliasPrecedence;
  public GlobalReferable underlyingReferable;

  public MetaReferable(Precedence precedence, String name, ModuleLocation location, Precedence aliasPrec, String aliasName, String description, MetaDefinition definition, MetaResolver resolver) {
    myPrecedence = precedence;
    myName = name;
    myLocation = location;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrec == null ? Precedence.DEFAULT : aliasPrec;
    this.description = description;
    myDefinition = definition;
    myResolver = resolver;
  }

  public MetaReferable(Precedence precedence, String name, ModuleLocation location, String description, MetaDefinition definition, MetaResolver resolver) {
    this(precedence, name, location, null, null, description, definition, resolver);
  }

  @Override
  public @NotNull ModuleLocation getLocation() {
    return myLocation;
  }

  @Override
  public @Nullable LocatedReferable getLocatedReferableParent() {
    return null;
  }

  @Nullable
  @Override
  @Contract(pure = true)
  public MetaDefinition getDefinition() {
    return myDefinition;
  }

  public void setDefinition(@NotNull MetaDefinition definition) {
    myDefinition = definition;
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
