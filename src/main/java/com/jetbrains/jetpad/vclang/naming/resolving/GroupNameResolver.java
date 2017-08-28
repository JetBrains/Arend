package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;

import java.util.ArrayList;

public class GroupNameResolver<T> {
  private final DefinitionResolveNameVisitor<T> myVisitor;
  private final TypecheckableProvider<T> myTypecheckableProvider;

  public GroupNameResolver(NameResolver nameResolver, ParserInfoProvider definitionProvider, ErrorReporter<T> errorReporter, TypecheckableProvider<T> provider) {
    myVisitor = new DefinitionResolveNameVisitor<>(nameResolver, definitionProvider, errorReporter);
    myTypecheckableProvider = provider;
  }

  public void resolveGroup(Group group, Scope parentScope) {
    Scope staticScope = new NamespaceScope(myVisitor.getNameResolver().nsProviders.statics.forReferable(group.getReferable()));
    Scope dynamicScope = new NamespaceScope(myVisitor.getNameResolver().nsProviders.dynamics.forReferable(group.getReferable()));
    MergeScope cmdScope = new MergeScope(new ArrayList<>());
    for (NamespaceCommand cmd : group.getNamespaceCommands()) {
      Scope scope = cmd.openedScope(parentScope, myVisitor.getNameResolver(), myVisitor.getErrorReporter());
      if (scope != null) {
        cmdScope.addScope(scope, myVisitor.getErrorReporter(), cmd);
      }
    }

    Scope scope = new MergeScope(staticScope, dynamicScope, cmdScope);
    Concrete.ReferableDefinition<T> def = myTypecheckableProvider.getTypecheckable(group.getReferable());
    if (def instanceof Concrete.Definition) {
      ((Concrete.Definition<T>) def).accept(myVisitor, scope);
    }
    for (Group subgroup : group.getStaticSubgroups()) {
      resolveGroup(subgroup, scope);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, scope);
    }
  }
}
