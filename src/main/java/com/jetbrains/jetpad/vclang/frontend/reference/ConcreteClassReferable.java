package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.CachingScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteGlobalReferable implements ClassReferable {
  private final ChildGroup myGroup;
  private final Collection<? extends InternalConcreteGlobalReferable> myFields;
  private final Collection<? extends Concrete.ReferenceExpression> mySuperClasses;

  public ConcreteClassReferable(Position position, @Nonnull String name, Precedence precedence, Collection<? extends InternalConcreteGlobalReferable> fields, Collection<? extends Concrete.ReferenceExpression> superClasses, ChildGroup group) {
    super(position, name, precedence);
    myFields = fields;
    mySuperClasses = superClasses;
    myGroup = group;
  }

  @Nonnull
  @Override
  public Collection<? extends ClassReferable> getSuperClassReferences() {
    if (mySuperClasses.isEmpty()) {
      return Collections.emptyList();
    }

    Scope scope = CachingScope.make(myGroup.getGroupScope());
    List<ClassReferable> superClasses = new ArrayList<>(mySuperClasses.size());
    for (Concrete.ReferenceExpression superClass : mySuperClasses) {
      Referable referable = superClass.getReferent();
      if (referable instanceof UnresolvedReference) {
        referable = ((UnresolvedReference) referable).resolve(scope);
      }
      if (referable instanceof ClassReferable) {
        superClasses.add((ClassReferable) referable);
      }
    }
    return superClasses;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalConcreteGlobalReferable> getFieldReferables() {
    return myFields;
  }
}
