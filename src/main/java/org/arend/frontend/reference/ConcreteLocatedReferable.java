package org.arend.frontend.reference;

import org.arend.error.SourceInfo;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.parser.Position;
import org.arend.module.ModulePath;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.DataContainer;
import org.arend.naming.reference.LocatedReferableImpl;
import org.arend.naming.reference.TCReferable;
import org.arend.term.Precedence;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConcreteLocatedReferable extends LocatedReferableImpl implements SourceInfo, DataContainer {
  private final Position myPosition;
  private Concrete.ReferableDefinition myDefinition;

  public ConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, TCReferable parent, Kind kind) {
    super(precedence, name, parent, kind);
    myPosition = position;
  }

  public ConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, ModulePath modulePath) {
    super(precedence, name, modulePath);
    myPosition = position;
  }

  @Nullable
  @Override
  public Position getData() {
    return myPosition;
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
