package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.term.abs.Abstract;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteLocatedReferable implements ClassReferable {
  private ChildGroup myGroup;
  private List<GlobalReferable> myDynamicReferables = Collections.emptyList();
  private final Collection<? extends ConcreteClassFieldReferable> myFields;
  private final List<? extends Reference> myUnresolvedSuperClasses;
  private final List<ClassReferable> mySuperClasses;
  private boolean myResolved = false;

  public ConcreteClassReferable(Object data, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, Collection<? extends ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, LocatedReferable parent) {
    super(data, name, precedence, aliasName, aliasPrecedence, parent, Kind.CLASS);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
  }

  public void setGroup(ChildGroup group) {
    myGroup = group;
    Collection<? extends Group> subgroups = group.getDynamicSubgroups();
    if (!subgroups.isEmpty()) {
      myDynamicReferables = new ArrayList<>();
      for (Group subgroup : subgroups) {
        LocatedReferable ref = subgroup.getReferable();
        if (!(ref instanceof ConcreteLocatedReferable && ((ConcreteLocatedReferable) ref).getDefinition() instanceof Concrete.CoClauseFunctionDefinition)) {
          myDynamicReferables.add(ref);
        }
      }
    }
  }

  @Override
  public Concrete.ClassDefinition getDefinition() {
    return (Concrete.ClassDefinition) super.getDefinition();
  }

  @NotNull
  @Override
  public List<? extends ClassReferable> getSuperClassReferences() {
    if (myUnresolvedSuperClasses.isEmpty()) {
      return Collections.emptyList();
    }

    resolve();
    return mySuperClasses;
  }

  @Override
  public boolean hasLevels(int index) {
    List<Concrete.ReferenceExpression> superClasses = getDefinition().getSuperClasses();
    return index < superClasses.size() && (superClasses.get(index).getPLevels() != null || superClasses.get(index).getHLevels() != null);
  }

  protected void resolve() {
    if (!myResolved) {
      ChildGroup parent = myGroup.getParentGroup();
      resolve(CachingScope.make(parent == null ? ScopeFactory.forGroup(myGroup, EmptyModuleScopeProvider.INSTANCE) : LexicalScope.insideOf(myGroup, parent.getGroupScope(), LexicalScope.Extent.ONLY_EXTERNAL)));
      myResolved = true;
    }
  }

  protected void resolve(Scope scope) {
    mySuperClasses.clear();
    for (Reference superClass : myUnresolvedSuperClasses) {
      Referable ref = ExpressionResolveNameVisitor.resolve(superClass.getReferent(), scope, true, null);
      if (ref instanceof ClassReferable) {
        mySuperClasses.add((ClassReferable) ref);
      }
    }
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
  public @NotNull Collection<? extends GlobalReferable> getDynamicReferables() {
    return myDynamicReferables;
  }

  @Override
  public boolean isRecord() {
    return getDefinition().isRecord();
  }

  @Override
  public @Nullable Abstract.LevelParameters getPLevelParameters() {
    return getDefinition().getPLevelParameters();
  }

  @Override
  public @Nullable Abstract.LevelParameters getHLevelParameters() {
    return getDefinition().getHLevelParameters();
  }
}
