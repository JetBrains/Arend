package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteResolvedClassReferable implements ClassReferable {
  private ChildGroup myGroup;
  private final List<? extends Reference> myUnresolvedSuperClasses;
  private boolean myResolved = false;

  public ConcreteClassReferable(Object data, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, List<ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, LocatedReferable parent) {
    super(data, name, precedence, aliasName, aliasPrecedence, parent, fields);
    myUnresolvedSuperClasses = superClasses;
  }

  public void setGroup(ChildGroup group) {
    myGroup = group;
    Collection<? extends Group> subgroups = group.getDynamicSubgroups();
    if (!subgroups.isEmpty()) {
      dynamicReferables = new ArrayList<>();
      for (Group subgroup : subgroups) {
        LocatedReferable ref = subgroup.getReferable();
        if (!(ref instanceof ConcreteLocatedReferable && ((ConcreteLocatedReferable) ref).getDefinition() instanceof Concrete.CoClauseFunctionDefinition)) {
          dynamicReferables.add(ref);
        }
      }
    }
  }

  @Override
  protected boolean setFromConcrete() {
    return false;
  }

  @NotNull
  @Override
  public List<? extends ClassReferable> getSuperClassReferences() {
    if (myUnresolvedSuperClasses.isEmpty()) {
      return Collections.emptyList();
    }

    resolve();
    return superClasses;
  }

  protected void resolve() {
    if (!myResolved) {
      ChildGroup parent = myGroup.getParentGroup();
      resolve(CachingScope.make(parent == null ? ScopeFactory.forGroup(myGroup, EmptyModuleScopeProvider.INSTANCE, Scope.Kind.EXPR) : LexicalScope.insideOf(myGroup, parent.getGroupScope(), LexicalScope.Extent.ONLY_EXTERNAL, Scope.Kind.EXPR)));
      myResolved = true;
    }
  }

  protected void resolve(Scope scope) {
    superClasses.clear();
    for (Reference superClass : myUnresolvedSuperClasses) {
      Referable ref = ExpressionResolveNameVisitor.resolve(superClass.getReferent(), scope, true, null);
      if (ref instanceof ClassReferable) {
        superClasses.add((ClassReferable) ref);
      }
    }
  }
}
