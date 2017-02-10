package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongDefinition;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class NameResolver {
  private ModuleLoader myModuleLoader;

  public NameResolver() {
    this(null);
  }

  public NameResolver(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public void setModuleLoader(ModuleLoader moduleLoader) {
    myModuleLoader = moduleLoader;
  }

  public ModuleNamespace resolveModuleNamespace(final ModulePath modulePath, ModuleNamespaceProvider moduleNsProvider) {
    ModuleNamespace ns = resolveModuleNamespace_(modulePath, moduleNsProvider);
    if (myModuleLoader != null && (ns == null || ns.getRegisteredClass() == null)) {
      Abstract.ClassDefinition loadedClass = myModuleLoader.load(modulePath);
      if (ns == null && loadedClass != null) {
        ns = resolveModuleNamespace_(modulePath, moduleNsProvider);
      }
    }
    return ns;
  }

  private ModuleNamespace resolveModuleNamespace_(final ModulePath path, ModuleNamespaceProvider moduleNsProvider) {
    ModuleNamespace ns = moduleNsProvider.root();
    for (String name : path.toList()) {
      ns = ns.getSubmoduleNamespace(name);
      if (ns == null) {
        break;
      }
    }
    return ns;
  }

  public Abstract.Definition resolveDefinition(final Scope currentScope, final List<String> path, StaticNamespaceProvider staticNsProvider) {
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
        scope = new NamespaceScope(staticNsProvider.forDefinition(ref));
      }
      return ref;
    }
  }

  public Abstract.Definition resolveDefCall(final Scope currentScope, final Abstract.DefCallExpression defCall, ModuleNamespaceProvider moduleNsProvider, StaticNamespaceProvider staticNsProvider) {
    if (defCall.getReferent() != null) {
      return defCall.getReferent();
    }
    if (defCall.getName() == null) {
      throw new IllegalArgumentException();
    }

    if (defCall.getExpression() == null) {
      return currentScope.resolveName(defCall.getName());
    } else if (defCall.getExpression() instanceof Abstract.DefCallExpression) {
      Abstract.Definition exprTarget = resolveDefCall(currentScope, (Abstract.DefCallExpression) defCall.getExpression(), moduleNsProvider, staticNsProvider);
      final Namespace ns;
      if (exprTarget != null) {
        ns = staticNsProvider.forDefinition(exprTarget);
      } else {
        // TODO: implement this coherently
        // ns = resolveModuleNamespace((Abstract.DefCallExpression) defCall.getExpression());
        ns = null;
      }
      // TODO: throw MemberNotFoundError
      return ns != null ? ns.resolveName(defCall.getName()) : null;
    } else if (defCall.getExpression() instanceof Abstract.ModuleCallExpression) {
      Abstract.Definition module = resolveModuleCall(currentScope, (Abstract.ModuleCallExpression) defCall.getExpression(), moduleNsProvider);
      if (module instanceof Abstract.ClassDefinition) {
        ModuleNamespace moduleNamespace = moduleNsProvider.forModule((Abstract.ClassDefinition) module);
        return moduleNamespace.resolveName(defCall.getName());
      }
      return null;
    } else {
      return null;
    }
  }

  public Abstract.Definition resolveModuleCall(final Scope currentScope, final Abstract.ModuleCallExpression moduleCall, ModuleNamespaceProvider moduleNsProvider) {
    if (moduleCall.getModule() != null) {
      return moduleCall.getModule();
    }

    if (moduleCall.getPath() == null) {
      throw new IllegalArgumentException();
    }
    ModuleNamespace ns = resolveModuleNamespace(moduleCall.getPath(), moduleNsProvider);
    return ns == null ? null : ns.getRegisteredClass();
  }

  public Abstract.ClassField resolveClassField(Abstract.ClassDefinition classDefinition, String name, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, Abstract.SourceNode cause) {
    Abstract.Definition resolvedRef = dynamicNsProvider.forClass(classDefinition).resolveName(name);
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
