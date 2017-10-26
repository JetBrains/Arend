package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.ReferenceError;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.naming.scope.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.abs.AbstractExpressionError;
import com.jetbrains.jetpad.vclang.typechecking.error.ProxyError;

import java.util.*;

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
      referable = ((UnresolvedReference) referable).resolve(parentScope);
    }

    if (!(referable instanceof GlobalReferable)) {
      if (referable instanceof ErrorReference) {
        myErrorReporter.report(new ProxyError(groupRef, ((ErrorReference) referable).getError()));
      } else if (!(referable instanceof UnresolvedReference)) {
        myErrorReporter.report(new ProxyError(groupRef, new ReferenceError("'" + origRef.textRepresentation() + "' is not a reference to a definition", origRef)));
      }
      return null;
    }

    return (GlobalReferable) referable;
  }

  private Scope getOpenedScope(NamespaceCommand cmd, Scope parentScope, GlobalReferable groupRef) {
    Referable referable = cmd.getGroupReference();
    Collection<? extends GlobalReferable> path = cmd.getImportedPath();
    NamespaceCommand.Kind kind = cmd.getKind();
    if ((kind == NamespaceCommand.Kind.IMPORT && path.isEmpty()) || (kind != NamespaceCommand.Kind.IMPORT && referable == null)) {
      myErrorReporter.report(new ProxyError(groupRef, AbstractExpressionError.incomplete(cmd)));
      return null;
    }

    GlobalReferable globalRef;
    if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
      List<String> namePath = new ArrayList<>(path.size());
      for (Referable ref : path) {
        namePath.add(ref.textRepresentation());
      }

      ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(new ModulePath(namePath));
      if (moduleNamespace == null) {
        myErrorReporter.report(new ProxyError(groupRef, new NotInScopeError(cmd, null, String.join(".", namePath))));
        return null;
      }
      globalRef = moduleNamespace.getRegisteredClass();
    } else {
      globalRef = resolveGlobal(referable, parentScope, groupRef);
    }
    if (globalRef == null) {
      return null;
    }

    Scope scope = new NamespaceScope(myNameResolver.nsProviders.statics.forReferable(globalRef));
    Collection<? extends NameRenaming> refs = cmd.getOpenedReferences();
    if (!refs.isEmpty() && !cmd.isUsing()) {
      Set<String> names = new HashSet<>();
      for (NameRenaming renaming : refs) {
        globalRef = renaming.getNewReferable();
        if (globalRef == null) {
          globalRef = resolveGlobal(renaming.getOldReference(), scope, groupRef);
        }
        if (globalRef != null) {
          names.add(globalRef.textRepresentation());
        }
      }
      scope = new FilteredScope(scope, names, true);
    }

    Collection<? extends Referable> hiddenRefs = cmd.getHiddenReferences();
    if (!hiddenRefs.isEmpty()) {
      Set<String> names = new HashSet<>();
      for (Referable ref : hiddenRefs) {
        globalRef = resolveGlobal(ref, scope, groupRef);
        if (globalRef != null) {
          names.add(globalRef.textRepresentation());
        }
      }
      scope = new FilteredScope(scope, names, false);
    }

    return scope;
  }
}
