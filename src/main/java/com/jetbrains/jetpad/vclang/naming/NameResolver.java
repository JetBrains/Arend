package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongDefinition;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class NameResolver {
  public final NamespaceProviders nsProviders;
  private ModuleResolver myModuleResolver;

  public NameResolver(NamespaceProviders nsProviders) {
    this(nsProviders,null);
  }

  public NameResolver(NamespaceProviders nsProviders, ModuleResolver moduleResolver) {
    this.nsProviders = nsProviders;
    myModuleResolver = moduleResolver;
  }

  public void setModuleResolver(ModuleResolver moduleResolver) {
    myModuleResolver = moduleResolver;
  }

  public ModuleNamespace resolveModuleNamespace(final ModulePath modulePath) {
    ModuleNamespace ns = resolveModuleNamespace_(modulePath);
    if (myModuleResolver != null && (ns == null || ns.getRegisteredClass() == null)) {
      Abstract.ClassDefinition loadedClass = myModuleResolver.load(modulePath);
      if (ns == null && loadedClass != null) {
        ns = resolveModuleNamespace_(modulePath);
      }
    }
    return ns;
  }

  private ModuleNamespace resolveModuleNamespace_(final ModulePath path) {
    ModuleNamespace ns = nsProviders.modules.root();
    for (String name : path.toList()) {
      ns = ns.getSubmoduleNamespace(name);
      if (ns == null) {
        break;
      }
    }
    return ns;
  }

  public Abstract.Definition resolveDefinition(final Scope currentScope, final List<String> path) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException();
    } else {
      Scope scope = currentScope;
      Abstract.Definition ref = null;
      for (String name : path) {
        ref = scope.resolveName(name);
        if (ref == null) {
          return null;
        }
        scope = new NamespaceScope(nsProviders.statics.forDefinition(ref));
      }
      return ref;
    }
  }

  public Abstract.Definition resolveDefCall(final Scope currentScope, final Abstract.DefCallExpression defCall) {
    if (defCall.getReferent() != null) {
      return defCall.getReferent();
    }
    if (defCall.getName() == null) {
      throw new IllegalArgumentException();
    }

    if (defCall.getExpression() == null) {
      return currentScope.resolveName(defCall.getName());
    } else if (defCall.getExpression() instanceof Abstract.DefCallExpression) {
      Abstract.Definition exprTarget = resolveDefCall(currentScope, (Abstract.DefCallExpression) defCall.getExpression());
      final Namespace ns;
      if (exprTarget != null) {
        ns = nsProviders.statics.forDefinition(exprTarget);
      } else {
        // TODO: implement this coherently
        // ns = resolveModuleNamespace((Abstract.DefCallExpression) defCall.getExpression());
        ns = null;
      }
      // TODO: throw MemberNotFoundError
      return ns != null ? ns.resolveName(defCall.getName()) : null;
    } else if (defCall.getExpression() instanceof Abstract.ModuleCallExpression) {
      Abstract.Definition module = resolveModuleCall(currentScope, (Abstract.ModuleCallExpression) defCall.getExpression());
      if (module != null) {
        Namespace moduleNamespace = nsProviders.statics.forDefinition(module);
        return moduleNamespace.resolveName(defCall.getName());
      }
      return null;
    } else {
      return null;
    }
  }

  public Abstract.ClassDefinition resolveModuleCall(final Scope currentScope, final Abstract.ModuleCallExpression moduleCall) {
    if (moduleCall.getModule() != null) {
      if (!(moduleCall.getModule() instanceof Abstract.ClassDefinition)) throw new IllegalStateException();
      return (Abstract.ClassDefinition) moduleCall.getModule();
    }

    if (moduleCall.getPath() == null) {
      throw new IllegalArgumentException();
    }
    ModuleNamespace ns = resolveModuleNamespace(moduleCall.getPath());
    return ns == null ? null : ns.getRegisteredClass();
  }

  public Abstract.ClassField resolveClassField(Abstract.ClassDefinition classDefinition, String name, ErrorReporter errorReporter, Abstract.SourceNode cause) {
    Abstract.Definition resolvedRef = nsProviders.dynamics.forClass(classDefinition).resolveName(name);
    if (resolvedRef instanceof Abstract.ClassField) {
      return (Abstract.ClassField) resolvedRef;
    } else {
      errorReporter.report(resolvedRef != null ? new WrongDefinition("Expected a class field", resolvedRef, cause) : new NotInScopeError(cause, name));
      return null;
    }
  }

  public Abstract.ClassField resolveClassFieldByView(Abstract.ClassView classView, String name, ErrorReporter errorReporter, Abstract.SourceNode cause) {
    if (name.equals(classView.getClassifyingFieldName())) {
      return classView.getClassifyingField();
    }
    for (Abstract.ClassViewField viewField : classView.getFields()) {
      if (name.equals(viewField.getName())) {
        return viewField.getUnderlyingField();
      }
    }

    errorReporter.report(new NotInScopeError(cause, name));
    return null;
  }

}
