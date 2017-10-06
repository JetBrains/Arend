package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.ReferenceError;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.naming.scope.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.abs.AbstractExpressionError;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GroupResolver {
  private final NameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;

  public GroupResolver(NameResolver nameResolver, ErrorReporter errorReporter) {
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
  }

  protected void processReferable(GlobalReferable referable, Scope scope) { }

  private Scope getGroupScope(Group group, Scope parentScope, boolean reportErrors) {
    GlobalReferable groupRef = group.getReferable();
    Scope staticScope = new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(groupRef));
    Scope dynamicScope = new NamespaceScope(myNameResolver.nsProviders.dynamics.forReferable(groupRef));
    Scope scope = new MergeScope(staticScope, dynamicScope, parentScope);
    if (group.getNamespaceCommands().isEmpty()) {
      return scope;
    }

    MergeScope cmdScope = new MergeScope(new ArrayList<>());
    for (NamespaceCommand cmd : group.getNamespaceCommands()) {
      Scope openedScope = getOpenedScope(cmd, scope, groupRef);
      if (openedScope != null) {
        cmdScope.addScope(openedScope, reportErrors ? myErrorReporter : DummyErrorReporter.INSTANCE, groupRef);
      }
    }
    return new MergeScope(staticScope, dynamicScope, cmdScope, parentScope);
  }

  public Scope getGroupScope(Group group, Scope parentScope) {
    return getGroupScope(group, parentScope, false);
  }

  public void resolveGroup(Group group, Scope parentScope) {
    Scope scope = getGroupScope(group, parentScope, true);
    processReferable(group.getReferable(), scope);
    for (Group subgroup : group.getSubgroups()) {
      resolveGroup(subgroup, scope);
    }
    for (GlobalReferable referable : group.getConstructors()) {
      processReferable(referable, scope);
    }
    for (GlobalReferable referable : group.getFields()) {
      processReferable(referable, scope);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      resolveGroup(subgroup, scope);
    }
  }

  private GlobalReferable resolveGlobal(Referable referable, Scope parentScope, GlobalReferable groupRef) {
    Referable origRef = referable;
    if (referable instanceof UnresolvedReference) {
      referable = ((UnresolvedReference) referable).resolve(parentScope, myNameResolver);
    }

    if (!(referable instanceof GlobalReferable)) {
      if (referable == null) {
        myErrorReporter.report(new ProxyError(groupRef, new NotInScopeError(origRef)));
      } else if (!(referable instanceof UnresolvedReference)) {
        myErrorReporter.report(new ProxyError(groupRef, new ReferenceError("'" + origRef.textRepresentation() + "' is not a reference to a definition", origRef)));
      }
      return null;
    }

    return (GlobalReferable) referable;
  }

  private Scope getOpenedScope(NamespaceCommand cmd, Scope parentScope, GlobalReferable groupRef) {
    Referable referable = cmd.getGroupReference();
    if (referable == null) {
      myErrorReporter.report(new ProxyError(groupRef, AbstractExpressionError.incomplete(cmd)));
      return null;
    }

    GlobalReferable globalRef = resolveGlobal(referable, parentScope, groupRef);
    if (globalRef == null) {
      return null;
    }

    Scope scope = new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(globalRef));
    Collection<? extends Referable> refs = cmd.getSubgroupReferences();
    if (refs != null) {
      Set<String> names = new HashSet<>();
      for (Referable ref : refs) {
        globalRef = resolveGlobal(ref, scope, groupRef);
        if (globalRef != null) {
          names.add(globalRef.textRepresentation());
        }
      }
      scope = new FilteredScope(scope, names, !cmd.isHiding());
    }

    return scope;
  }
}
