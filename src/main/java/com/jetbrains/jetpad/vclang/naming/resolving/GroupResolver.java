package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NamespaceError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class GroupResolver<T> {
  private final NameResolver myNameResolver;
  private final ErrorReporter<T> myErrorReporter;

  public GroupResolver(NameResolver nameResolver, ErrorReporter<T> errorReporter) {
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
  }

  protected abstract void processGroup(Group group, Scope scope);

  public void resolveGroup(Group group, Scope parentScope) {
    Scope staticScope = new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(group.getReferable()));
    Scope dynamicScope = new NamespaceScope(myNameResolver.nsProviders.dynamics.forReferable(group.getReferable()));
    MergeScope cmdScope = new MergeScope(new ArrayList<>());
    for (NamespaceCommand cmd : group.getNamespaceCommands()) {
      Scope scope = getOpenedScope(cmd, parentScope, myNameResolver, myErrorReporter);
      if (scope != null) {
        cmdScope.addScope(scope, myErrorReporter, cmd);
      }
    }

    Scope scope = new MergeScope(staticScope, dynamicScope, cmdScope, parentScope);
    processGroup(group, scope);
    for (Group subgroup : group.getSubgroups()) {
      resolveGroup(subgroup, scope);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, scope);
    }
  }

  private Scope getOpenedScope(NamespaceCommand cmd, Scope parentScope, NameResolver nameResolver, ErrorReporter<T> errorReporter) {
    Referable referable = cmd.getGroupReference();
    String refText = referable.textRepresentation();
    if (referable instanceof UnresolvedReference) {
      referable = ((UnresolvedReference) referable).resolve(parentScope, nameResolver);
    }

    if (!(referable instanceof GlobalReferable)) {
      errorReporter.report(new NamespaceError<>("'" + refText + "' is not a reference to a definition", cmd));
      return null;
    }

    Scope scope = new NamespaceScope(nameResolver.nsProviders.statics.forReferable((GlobalReferable) referable));
    Collection<? extends Referable> refs = cmd.getSubgroupReferences();
    if (refs != null) {
      Set<String> names = new HashSet<>();
      for (Referable ref : refs) {
        refText = ref.textRepresentation();
        if (ref instanceof UnresolvedReference) {
          if (!(((UnresolvedReference) ref).resolve(scope, nameResolver) instanceof GlobalReferable)) {
            errorReporter.report(new NamespaceError<>("'" + refText + "' is not a reference to a definition", cmd));
            continue;
          }
        }
        names.add(refText);
      }
      scope = new FilteredScope(scope, names, !cmd.isHiding());
    }

    return scope;
  }
}
