package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NameResolver {
  private final ModuleNamespaceProvider myModuleNamespaceProvider;
  private final StaticNamespaceProvider myStaticNamespaceProvider;

  public NameResolver(ModuleNamespaceProvider myModuleNamespaceProvider, StaticNamespaceProvider myStaticNamespaceProvider) {
    this.myModuleNamespaceProvider = myModuleNamespaceProvider;
    this.myStaticNamespaceProvider = myStaticNamespaceProvider;
  }

  public ModuleNamespace resolveModuleNamespace(final List<String> path) {
    ModuleNamespace ns = myModuleNamespaceProvider.root();
    for (String name : path) {
      ns = ns.getSubmoduleNamespace(name);
      if (ns == null) return null;
    }
    return ns;
  }

  public ModuleNamespace resolveModuleNamespace(final Abstract.DefCallExpression moduleCall) {
    if (moduleCall.getReferent() != null) {
      if (moduleCall.getReferent() instanceof Abstract.ClassDefinition) {
        return myModuleNamespaceProvider.forModule((Abstract.ClassDefinition) moduleCall.getReferent());
      } else if (moduleCall.getReferent() instanceof ClassDefinition) {
        return myModuleNamespaceProvider.forModule((ClassDefinition) moduleCall.getReferent());
      } else {
        return null;
      }
    }
    if (moduleCall.getName() == null) throw new IllegalArgumentException();

    final ModuleNamespace parentNs;
    if (moduleCall.getExpression() == null) {
      parentNs = myModuleNamespaceProvider.root();
    } else if (moduleCall.getExpression() instanceof Abstract.DefCallExpression) {
      parentNs = resolveModuleNamespace((Abstract.DefCallExpression) moduleCall.getExpression());
    } else {
      parentNs = null;
    }
    return parentNs != null ? parentNs.getSubmoduleNamespace(moduleCall.getName()) : null;
  }

  public Referable resolveDefinition(final Scope curretScope, final List<String> path) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException();
    } else {
      Scope scope = curretScope;
      Referable ref = null;
      for (String name : path) {
        ref = scope.resolveName(name);
        if (ref == null) return null;
        scope = staticNamespaceFor(ref);
      }
      return ref;
    }
  }

  public Referable resolveDefinition(final Scope curretScope, final String path) {
    return resolveDefinition(curretScope, Arrays.asList(path.split("\\.")));
  }

  public Referable resolveDefCall(final Scope curretScope, final Abstract.DefCallExpression defCall) {
    if (defCall.getReferent() != null) {
      return defCall.getReferent();
    }
    if (defCall.getName() == null) throw new IllegalArgumentException();

    if (defCall.getExpression() == null) {
      return curretScope.resolveName(defCall.getName());
    } else if (defCall.getExpression() instanceof Abstract.DefCallExpression) {
      Referable exprTarget = resolveDefCall(curretScope, (Abstract.DefCallExpression) defCall.getExpression());
      final Namespace ns;
      if (exprTarget != null) {
        ns = staticNamespaceFor(exprTarget);
      } else {
        // TODO: implement this coherently
        // ns = resolveModuleNamespace((Abstract.DefCallExpression) defCall.getExpression());
        ns = null;
      }
      // TODO: throw MemberNotFoundError
      return ns != null ? ns.resolveName(defCall.getName()) : null;
    } else {
      return null;
    }
  }

  public Referable resolveModuleCall(final Scope curretScope, final Abstract.ModuleCallExpression moduleCall) {
    if (moduleCall.getModule() != null) {
      return moduleCall.getModule();
    }
    if (moduleCall.getPath() == null) throw new IllegalArgumentException();

    ModuleNamespace ns = myModuleNamespaceProvider.root();
    for (String name : moduleCall.getPath()) {
      ns = ns.getSubmoduleNamespace(name);
    }
    return ns.getRegisteredClass();
  }

  public Namespace staticNamespaceFor(Referable ref) {
    if (ref instanceof Definition) {
      return ((Definition) ref).getNamespace();
    } else if (ref instanceof Abstract.Definition) {
      return myStaticNamespaceProvider.forDefinition((Abstract.Definition) ref);
    } else {
      throw new IllegalStateException();
    }
  }
}
