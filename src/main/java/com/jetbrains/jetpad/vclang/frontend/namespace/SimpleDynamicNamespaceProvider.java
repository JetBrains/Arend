package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  private final Map<GlobalReferable, SimpleNamespace> myNamespaces = new HashMap<>();
  private ConcreteProvider myConcreteProvider;
  private NameResolver myNameResolver;
  private ErrorReporter myErrorReporter;
  private boolean myHasSuperClasses;

  public SimpleDynamicNamespaceProvider(ConcreteProvider provider) {
    myConcreteProvider = provider;
  }

  public void setConcreteProvider(ConcreteProvider provider) {
    myConcreteProvider = provider;
  }

  @Nonnull
  @Override
  public Namespace forReferable(final GlobalReferable referable) {
    SimpleNamespace ns = myNamespaces.get(referable);
    return ns != null ? ns : EmptyNamespace.INSTANCE;
  }

  public void collect(Group group, ErrorReporter errorReporter, NameResolver nameResolver) {
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
    myHasSuperClasses = false;

    collectWithoutSuperClasses(group);
    if (myHasSuperClasses) {
      Set<GlobalReferable> updated = new HashSet<>();
      collectWithSupperClasses(group, EmptyScope.INSTANCE, updated);
    }
  }

  private void collectWithoutSuperClasses(Group group) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Group subgroup : group.getDynamicSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), DummyErrorReporter.INSTANCE);
    }
    for (GlobalReferable field : group.getFields()) {
      ns.addDefinition(field, DummyErrorReporter.INSTANCE);
    }
    myNamespaces.put(group.getReferable(), ns);

    for (Group subgroup : group.getSubgroups()) {
      collectWithoutSuperClasses(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collectWithoutSuperClasses(subgroup);
    }

    Concrete.ReferableDefinition definition = myConcreteProvider.getConcrete(group.getReferable());
    if (definition instanceof Concrete.ClassDefinition) {
      if (!((Concrete.ClassDefinition) definition).getSuperClasses().isEmpty()) {
        myHasSuperClasses = true;
      }
    }
  }

  private void collectWithSupperClasses(Group group, Scope parentScope, Set<GlobalReferable> updated) {
    Scope scope = new GroupResolver(myNameResolver, myErrorReporter).getGroupScope(group, parentScope);
    SimpleNamespace ns = new SimpleNamespace();
    if (!updateClass(group.getReferable(), new ExpressionResolveNameVisitor(scope, null, myNameResolver, myErrorReporter), new HashSet<>(), updated, ns)) {
      updated.add(group.getReferable());
      updateClassNamespace(group.getReferable(), ns);
    }

    for (Group subgroup : group.getSubgroups()) {
      collectWithSupperClasses(subgroup, scope, updated);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collectWithSupperClasses(subgroup, scope, updated);
    }
  }

  private boolean updateClass(GlobalReferable classRef, ExpressionResolveNameVisitor visitor, Set<GlobalReferable> current, Set<GlobalReferable> updated, SimpleNamespace result) {
    Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(classRef);
    if (!(def instanceof Concrete.ClassDefinition)) {
      return true;
    }

    Concrete.ClassDefinition classDef = (Concrete.ClassDefinition) def;
    boolean ok = true;
    current.add(classRef);
    for (Concrete.ReferenceExpression superClassRefExpr : classDef.getSuperClasses()) {
      visitor.visitReference(superClassRefExpr, null);
      Referable superClassRef = superClassRefExpr.getReferent();
      if (superClassRef instanceof GlobalReferable) {
        if (updated.contains(superClassRef)) {
          result.addAll(myNamespaces.get(superClassRef), myErrorReporter);
        } else if (current.contains(superClassRef)) {
          ok = false;
        } else {
          SimpleNamespace superClassNs = new SimpleNamespace();
          if (!updateClass((GlobalReferable) superClassRef, visitor, current, updated, superClassNs)) {
            ok = false;
          }
          result.addAll(superClassNs, myErrorReporter);
        }
      }
    }

    current.remove(classRef);
    if (ok) {
      updated.add(classRef);
      updateClassNamespace(classRef, result);
    }
    return ok;
  }

  private void updateClassNamespace(GlobalReferable classRef, SimpleNamespace newNs) {
    SimpleNamespace oldNs = myNamespaces.get(classRef);
    newNs.addAll(oldNs, myErrorReporter);
    myNamespaces.put(classRef, newNs);
  }
}
