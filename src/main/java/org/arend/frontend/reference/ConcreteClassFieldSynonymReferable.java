package org.arend.frontend.reference;

import org.arend.frontend.parser.Position;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.Reference;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.term.Precedence;
import org.arend.term.Precedence;

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
