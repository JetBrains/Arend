package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class ConcreteClassSynonymReferable extends ConcreteClassReferable {
  private Referable myUnderlyingClassReference;

  public ConcreteClassSynonymReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends InternalConcreteLocatedReferable> fields, Collection<? extends Concrete.ReferenceExpression> superClasses, ChildGroup group, TCReferable parent, Referable underlyingClassReference) {
    super(position, name, precedence, fields, superClasses, group, parent);
    myUnderlyingClassReference = underlyingClassReference;
  }

  public ConcreteClassSynonymReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends InternalConcreteLocatedReferable> fields, Collection<? extends Concrete.ReferenceExpression> superClasses, ChildGroup group, ModulePath parent, Referable myUnderlyingClassReference) {
    super(position, name, precedence, fields, superClasses, group, parent);
    this.myUnderlyingClassReference = myUnderlyingClassReference;
  }

  @Nullable
  @Override
  public TCClassReferable getUnderlyingReference() {
    return myUnderlyingClassReference instanceof TCClassReferable ? (TCClassReferable) myUnderlyingClassReference : null;
  }

  public Referable resolveUnderlyingReference(Scope scope) {
    myUnderlyingClassReference = ExpressionResolveNameVisitor.resolve(myUnderlyingClassReference, scope);
    return myUnderlyingClassReference;
  }
}
