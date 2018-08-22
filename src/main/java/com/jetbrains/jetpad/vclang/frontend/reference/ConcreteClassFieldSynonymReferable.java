package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.Reference;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConcreteClassFieldSynonymReferable extends ConcreteClassFieldReferable {
  private TCReferable myUnderlyingFieldReference = TCClassReferable.NULL_REFERABLE;
  private final Reference myUnresolvedUnderlyingFieldReference;

  public ConcreteClassFieldSynonymReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, ConcreteClassReferable parent, Reference underlyingFieldReference) {
    super(position, name, precedence, isVisible, true, parent, Kind.FIELD);
    myUnresolvedUnderlyingFieldReference = underlyingFieldReference;
  }

  @Nullable
  @Override
  public ConcreteClassReferable getLocatedReferableParent() {
    return (ConcreteClassReferable) super.getLocatedReferableParent();
  }

  @Nullable
  @Override
  public TCReferable getUnderlyingReference() {
    if (myUnderlyingFieldReference == TCClassReferable.NULL_REFERABLE) {
      ConcreteClassReferable parent = getLocatedReferableParent();
      TCClassReferable classRef = parent == null ? null : parent.getUnderlyingReference();
      Referable resolved = classRef == null ? null : ExpressionResolveNameVisitor.resolve(myUnresolvedUnderlyingFieldReference.getReferent(), new ClassFieldImplScope(classRef, false), true);
      myUnderlyingFieldReference = resolved instanceof TCReferable ? (TCReferable) resolved : null;
    }
    return myUnderlyingFieldReference;
  }

  @Nullable
  @Override
  public Reference getUnresolvedUnderlyingReference() {
    return myUnresolvedUnderlyingFieldReference;
  }

  @Override
  public boolean isFieldSynonym() {
    return true;
  }
}
