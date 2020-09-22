package org.arend.naming.reference;

import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.module.ModuleLocation;
import org.arend.term.concrete.DefinableMetaDefinition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MetaReferable implements TCReferable, MetaRef {
  private final Precedence myPrecedence;
  private final String myName;
  private MetaDefinition myDefinition;
  private final MetaResolver myResolver;
  public final String description;
  private final String myAliasName;
  private final Precedence myAliasPrecedence;
  public Supplier<GlobalReferable> underlyingReferable;
  private final LocatedReferable myParent;

  public MetaReferable(Precedence precedence, String name, Precedence aliasPrec, String aliasName, String description, MetaDefinition definition, MetaResolver resolver, LocatedReferable parent) {
    myPrecedence = precedence;
    myName = name;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrec == null ? Precedence.DEFAULT : aliasPrec;
    this.description = description;
    myDefinition = definition;
    myResolver = resolver;
    myParent = parent;
  }

  public MetaReferable(Precedence precedence, String name, String description, MetaDefinition definition, MetaResolver resolver, LocatedReferable parent) {
    this(precedence, name, null, null, description, definition, resolver, parent);
  }

  @Override
  public @Nullable ModuleLocation getLocation() {
    return myParent == null ? null : myParent.getLocation();
  }

  @Override
  @Contract(pure = true)
  public @Nullable LocatedReferable getLocatedReferableParent() {
    return myParent;
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
    GlobalReferable result = underlyingReferable == null ? null : underlyingReferable.get();
    return result == null ? this : result;
  }

  @Override
  public @Nullable Object getData() {
    return getUnderlyingReferable();
  }

  @Override
  public boolean isTypechecked() {
    // If it's a definable meta, we always need to typecheck its dependencies
    return !(myDefinition instanceof DefinableMetaDefinition);
  }
}
