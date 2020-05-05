package org.arend.frontend.reference;

import org.arend.ext.error.SourceInfo;
import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.parser.Position;
import org.arend.module.FullModulePath;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.LocatedReferableImpl;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.resolving.visitor.TypeClassReferenceExtractVisitor;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConcreteLocatedReferable extends LocatedReferableImpl implements SourceInfo, DataContainer {
  private final Position myPosition;
  private final String myAliasName;
  private final Precedence myAliasPrecedence;
  private Concrete.ReferableDefinition myDefinition;

  public ConcreteLocatedReferable(Position position, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, TCReferable parent, Kind kind) {
    super(precedence, name, parent, kind);
    myPosition = position;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrecedence;
  }

  public ConcreteLocatedReferable(Position position, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, FullModulePath modulePath, Kind kind) {
    super(precedence, name, modulePath, kind);
    myPosition = position;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrecedence;
  }

  @Nullable
  @Override
  public Position getData() {
    return myPosition;
  }

  @Override
  public @Nullable String getAliasName() {
    return myAliasName;
  }

  @Override
  public @NotNull Precedence getAliasPrecedence() {
    return myAliasPrecedence;
  }

  public Concrete.ReferableDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public TCReferable getTypecheckable() {
    return myDefinition == null ? null : myDefinition.getRelatedDefinition().getData();
  }

  public void setDefinition(Concrete.ReferableDefinition definition) {
    assert myDefinition == null;
    myDefinition = definition;
  }

  @Override
  public String moduleTextRepresentation() {
    return myPosition == null ? null : myPosition.moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    return myPosition == null ? null : myPosition.positionTextRepresentation();
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myDefinition == null ? null : myDefinition.accept(new TypeClassReferenceExtractVisitor(ConcreteReferableProvider.INSTANCE), null);
  }
}
