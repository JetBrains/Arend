package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConcreteClassFieldSynonymReferable extends InternalConcreteLocatedReferable {
  private Referable myUnderlyingFieldReference;

  public ConcreteClassFieldSynonymReferable(Position position, @Nonnull String name, Precedence precedence, boolean isVisible, TCReferable parent, Referable underlyingFieldReference) {
    super(position, name, precedence, isVisible, parent);
    myUnderlyingFieldReference = underlyingFieldReference;
  }

  @Nullable
  @Override
  public TCReferable getUnderlyingReference() {
    return myUnderlyingFieldReference instanceof TCReferable ? (TCReferable) myUnderlyingFieldReference : null;
  }

  public Referable resolveUnderlyingReference(Scope scope) {
    myUnderlyingFieldReference = ExpressionResolveNameVisitor.resolve(myUnderlyingFieldReference, scope);
    return myUnderlyingFieldReference;
  }
}
