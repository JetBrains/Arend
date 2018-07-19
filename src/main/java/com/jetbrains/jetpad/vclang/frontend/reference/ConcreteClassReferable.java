package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.Reference;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteLocatedReferable implements TCClassReferable {
  private final ChildGroup myGroup;
  private final Collection<? extends InternalConcreteLocatedReferable> myFields;
  private final List<? extends Reference> myUnresolvedSuperClasses;
  private final List<TCClassReferable> mySuperClasses;
  private boolean myResolved = false;

  public ConcreteClassReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends InternalConcreteLocatedReferable> fields, List<? extends Reference> superClasses, ChildGroup group, TCReferable parent) {
    super(position, name, precedence, parent, Kind.TYPECHECKABLE);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
    myGroup = group;
  }

  public ConcreteClassReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends InternalConcreteLocatedReferable> fields, List<? extends Reference> superClasses, ChildGroup group, ModulePath parent) {
    super(position, name, precedence, parent);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
    myGroup = group;
  }

  @Nonnull
  @Override
  public Collection<? extends TCClassReferable> getSuperClassReferences() {
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
      Referable ref = ExpressionResolveNameVisitor.resolve(superClass.getReferent(), scope, true);
      if (ref instanceof TCClassReferable) {
        mySuperClasses.add((TCClassReferable) ref);
      }
    }
  }

  @Nonnull
  @Override
  public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
    return myUnresolvedSuperClasses;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalConcreteLocatedReferable> getFieldReferables() {
    return myFields;
  }

  @Nullable
  @Override
  public TCClassReferable getUnderlyingReference() {
    return null;
  }
}
