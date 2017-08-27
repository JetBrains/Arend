package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceError;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.provider.ParserInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ModifiableTypecheckableProvider;

import java.util.*;

public class GroupNameResolver<T> {
  private final DefinitionResolveNameVisitor<T> myVisitor;
  private final ModifiableTypecheckableProvider<T> myTypecheckableProvider;

  public GroupNameResolver(NameResolver nameResolver, ParserInfoProvider definitionProvider, ErrorReporter<T> errorReporter, ModifiableTypecheckableProvider<T> provider) {
    myVisitor = new DefinitionResolveNameVisitor<>(nameResolver, definitionProvider, errorReporter);
    myTypecheckableProvider = provider;
  }

  public void resolveGroup(Group group, Scope parentScope) {
    Scope staticScope = new NamespaceScope(myVisitor.getNameResolver().nsProviders.statics.forReferable(group.getReferable()));
    Scope dynamicScope = new NamespaceScope(myVisitor.getNameResolver().nsProviders.dynamics.forReferable(group.getReferable()));
    MergeScope cmdScope = new MergeScope(new ArrayList<>());
    for (Group.NamespaceCommand cmd : group.getNamespaceCommands()) {
      Scope scope = processOpenCommand(cmd, parentScope);
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

  public Scope processOpenCommand(Group.NamespaceCommand cmd, Scope parentScope) {
    Referable referable = cmd.getGroupReference();
    String refText = referable.textRepresentation();
    List<String> path = null;
    if (referable instanceof UnresolvedReference) {
      if (referable instanceof LongUnresolvedReference) {
        path = ((LongUnresolvedReference) referable).getPath();
      }
      referable = parentScope.resolveName(((UnresolvedReference) referable).getName());
    } else if (referable instanceof ModuleUnresolvedReference) {
      path = ((ModuleUnresolvedReference) referable).getPath();
      ModuleNamespace moduleNamespace = myVisitor.getNameResolver().resolveModuleNamespace(((ModuleUnresolvedReference) referable).getModulePath());
      referable = moduleNamespace != null ? moduleNamespace.getRegisteredClass() : null;
    }

    if (path != null && referable instanceof GlobalReferable) {
      for (String name : path) {
        referable = myVisitor.getNameResolver().nsProviders.statics.forReferable((GlobalReferable) referable).resolveName(name);
        if (referable == null) {
          break;
        }
      }
    }

    if (!(referable instanceof GlobalReferable)) {
      myVisitor.getErrorReporter().report(new NamespaceError<>("'" + refText + "' is not a reference to a definition", cmd));
      return null;
    }

    Scope scope = new NamespaceScope(myVisitor.getNameResolver().nsProviders.statics.forReferable((GlobalReferable) referable));
    Collection<? extends Referable> refs = cmd.getSubgroupReferences();
    if (refs != null) {
      Set<String> names = new HashSet<>();
      for (Referable ref : refs) {
        names.add(ref.textRepresentation());
      }
      scope = new FilteredScope(scope, names, !cmd.isHiding());
    }

    return scope;
  }
}
