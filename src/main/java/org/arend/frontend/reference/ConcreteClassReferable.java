package org.arend.frontend.reference;

import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.Reference;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteLocatedReferable implements TCClassReferable {
  private final ChildGroup myGroup;
  private final Collection<? extends ConcreteClassFieldReferable> myFields;
  private final List<? extends Reference> myUnresolvedSuperClasses;
  private final List<TCClassReferable> mySuperClasses;
  private boolean myResolved = false;

  public ConcreteClassReferable(Position position, @NotNull String name, Precedence precedence, Collection<? extends ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, ChildGroup group, TCReferable parent) {
    super(position, name, precedence, parent, Kind.TYPECHECKABLE);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
    myGroup = group;
  }

  public ConcreteClassReferable(Position position, @NotNull String name, Precedence precedence, Collection<? extends ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, ChildGroup group, ModulePath parent) {
    super(position, name, precedence, parent, Kind.TYPECHECKABLE);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
    myGroup = group;
  }

  @Override
  public Concrete.ClassDefinition getDefinition() {
    return (Concrete.ClassDefinition) super.getDefinition();
  }

  @NotNull
  @Override
  public List<? extends TCClassReferable> getSuperClassReferences() {
    if (myUnresolvedSuperClasses.isEmpty()) {
      return Collections.emptyList();
    }

    resolve();
    return mySuperClasses;
  }

  protected void resolve() {
    if (!myResolved) {
      resolve(CachingScope.make(myGroup.getGroupScope()));
      myResolved = true;
    }
  }

  protected void resolve(Scope scope) {
    mySuperClasses.clear();
    for (Reference superClass : myUnresolvedSuperClasses) {
      Referable ref = ExpressionResolveNameVisitor.resolve(superClass.getReferent(), scope, true, null);
      if (ref instanceof TCClassReferable) {
        mySuperClasses.add((TCClassReferable) ref);
      }
    }
  }

  @NotNull
  @Override
  public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
    return myUnresolvedSuperClasses;
  }

  @NotNull
  @Override
  public Collection<? extends ConcreteClassFieldReferable> getFieldReferables() {
    return myFields;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    List<Referable> result = new ArrayList<>();
    for (Concrete.ClassElement element : getDefinition().getElements()) {
      if (element instanceof Concrete.ClassFieldImpl) {
        result.add(((Concrete.ClassFieldImpl) element).getImplementedField());
      }
    }
    return result;
  }

  @Override
  public boolean isRecord() {
    return getDefinition().isRecord();
  }
}
