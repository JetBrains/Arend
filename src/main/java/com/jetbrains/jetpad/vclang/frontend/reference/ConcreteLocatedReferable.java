package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferableImpl;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConcreteLocatedReferable extends LocatedReferableImpl implements SourceInfo {
  private final Position myPosition;
  private Concrete.ReferableDefinition myDefinition;

  public ConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, TCReferable parent, boolean isTypecheckable) {
    super(precedence, name, parent, isTypecheckable);
    myPosition = position;
  }

  public ConcreteLocatedReferable(Position position, @Nonnull String name, Precedence precedence, ModulePath modulePath) {
    super(precedence, name, modulePath);
    myPosition = position;
  }

  public Concrete.ReferableDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public TCReferable getTypecheckable() {
    return myDefinition.getRelatedDefinition().getData();
  }

  public boolean isTypecheckable() {
    return myDefinition.getRelatedDefinition() == myDefinition;
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
    return myDefinition.accept(new TypeClassReferenceExtractVisitor(ConcreteReferableProvider.INSTANCE), null);
  }
}
