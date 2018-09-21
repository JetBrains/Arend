package org.arend.frontend.reference;

import org.arend.frontend.parser.Position;
import org.arend.module.ModulePath;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.Reference;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.Scope;
import org.arend.term.Precedence;
import org.arend.term.group.ChildGroup;

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
