package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.resolving.GroupResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  private final Map<GlobalReferable, SimpleNamespace> myNamespaces = new HashMap<>();
  private final TypecheckableProvider myTypecheckableProvider;
  private NameResolver myNameResolver;
  private ErrorReporter myErrorReporter;
  private boolean myHasSuperClasses;

  public SimpleDynamicNamespaceProvider(TypecheckableProvider provider) {
    myTypecheckableProvider = provider;
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

    collect1(group);
    if (myHasSuperClasses) {
      collect2(group, new EmptyScope());
    }
  }

  private void collect1(Group group) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Group subgroup : group.getDynamicSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), myErrorReporter);
    }
    for (GlobalReferable field : group.getFields()) {
      ns.addDefinition(field, myErrorReporter);
    }
    myNamespaces.put(group.getReferable(), ns);

    for (Group subgroup : group.getSubgroups()) {
      collect1(subgroup);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collect1(subgroup);
    }

    Concrete.ReferableDefinition definition = myTypecheckableProvider.getTypecheckable(group.getReferable());
    if (definition instanceof Concrete.ClassDefinition) {
      if (!((Concrete.ClassDefinition) definition).getSuperClasses().isEmpty()) {
        myHasSuperClasses = true;
      }
    }
  }

  private void collect2(Group group, Scope parentScope) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Group subgroup : group.getDynamicSubgroups()) {
      ns.addDefinition(subgroup.getReferable(), myErrorReporter);
    }
    for (GlobalReferable field : group.getFields()) {
      ns.addDefinition(field, myErrorReporter);
    }
    myNamespaces.put(group.getReferable(), ns);

    Scope scope = new GroupResolver(myNameResolver, myErrorReporter).getGroupScope(group, parentScope);
    Concrete.ReferableDefinition definition = myTypecheckableProvider.getTypecheckable(group.getReferable());
    if (definition instanceof Concrete.ClassDefinition) {
      List<? extends Concrete.ReferenceExpression<?>> superClasses = ((Concrete.ClassDefinition<?>) definition).getSuperClasses();
      if (!superClasses.isEmpty()) {
        SimpleNamespace newNs = new SimpleNamespace();
        ExpressionResolveNameVisitor exprResolver = new ExpressionResolveNameVisitor(scope, null, myNameResolver, null, myErrorReporter);
        for (Concrete.ReferenceExpression<?> superClassRef : ((Concrete.ClassDefinition<?>) definition).getSuperClasses()) {
          exprResolver.visitReference(superClassRef, null);
          if (superClassRef.getReferent() instanceof GlobalReferable) {
            Concrete.ReferableDefinition superClass = myTypecheckableProvider.getTypecheckable((GlobalReferable) superClassRef.getReferent());
            if (superClass instanceof Concrete.ClassDefinition) {
              for (Concrete.ClassField<?> field : ((Concrete.ClassDefinition<?>) superClass).getFields()) {
                newNs.addDefinition(field.getReferable(), myErrorReporter);
              }
            }
          }
        }

        SimpleNamespace oldNs = myNamespaces.get(group.getReferable());
        newNs.addAll(oldNs, myErrorReporter);
        myNamespaces.put(group.getReferable(), newNs);
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      collect2(subgroup, scope);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      collect2(subgroup, scope);
    }
  }
}
