package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.Reference;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassSynonymReferable extends ConcreteClassReferable {
  private TCClassReferable myUnderlyingClassReference = null;
  private final Reference myUnresolvedUnderlyingClassReference;

  public ConcreteClassSynonymReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends ConcreteClassFieldSynonymReferable> fields, List<? extends Reference> superClasses, ChildGroup group, TCReferable parent, Reference underlyingClassReference) {
    super(position, name, precedence, fields, superClasses, group, parent);
    myUnresolvedUnderlyingClassReference = underlyingClassReference;
  }

  public ConcreteClassSynonymReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends ConcreteClassFieldSynonymReferable> fields, List<? extends Reference> superClasses, ChildGroup group, ModulePath parent, Reference underlyingClassReference) {
    super(position, name, precedence, fields, superClasses, group, parent);
    myUnresolvedUnderlyingClassReference = underlyingClassReference;
  }

  @Nullable
  @Override
  public TCClassReferable getUnderlyingReference() {
    resolve();
    return myUnderlyingClassReference;
  }

  @Override
  protected void resolve(Scope scope) {
    super.resolve(scope);

    Referable resolved = ExpressionResolveNameVisitor.resolve(myUnresolvedUnderlyingClassReference.getReferent(), scope, true);
    if (resolved instanceof TCClassReferable) {
      myUnderlyingClassReference = (TCClassReferable) resolved;
    }
  }

  @Nullable
  @Override
  public Reference getUnresolvedUnderlyingReference() {
    return myUnresolvedUnderlyingClassReference;
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    return Collections.emptyList();
  }
}
