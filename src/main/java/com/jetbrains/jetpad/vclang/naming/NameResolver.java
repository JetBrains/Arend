package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.frontend.resolving.NamespaceProviders;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.NamespaceScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;

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

  public Abstract.GlobalReferableSourceNode resolveDefinition(final Scope currentScope, final List<String> path) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException();
    } else {
      Scope scope = currentScope;
      Abstract.ReferableSourceNode ref = null;
      for (String name : path) {
        ref = scope.resolveName(name);
        if (!(ref instanceof Abstract.GlobalReferableSourceNode)) {
          return null;
        }
        scope = new NamespaceScope(nsProviders.statics.forReferable((Abstract.GlobalReferableSourceNode) ref));
      }
      return (Abstract.GlobalReferableSourceNode) ref;
    }
  }

  public Abstract.ReferableSourceNode resolveReference(final Scope currentScope, final Concrete.ReferenceExpression reference) {
    if (reference.getReferent() != null) {
      return reference.getReferent();
    }
    if (reference.getName() == null) {
      throw new IllegalArgumentException();
    }

    if (reference.getExpression() == null) {
      return currentScope.resolveName(reference.getName());
    } else if (reference.getExpression() instanceof Concrete.ReferenceExpression) {
      Abstract.ReferableSourceNode exprTarget = resolveReference(currentScope, (Concrete.ReferenceExpression) reference.getExpression());
      final Namespace ns;
      if (exprTarget instanceof Abstract.GlobalReferableSourceNode) {
        ns = nsProviders.statics.forReferable((Abstract.GlobalReferableSourceNode) exprTarget);
      } else {
        // TODO: implement this coherently
        // ns = resolveModuleNamespace((Abstract.DefCallExpression) reference.getExpression());
        ns = null;
      }
      // TODO: throw MemberNotFoundError
      return ns != null ? ns.resolveName(reference.getName()) : null;
    } else if (reference.getExpression() instanceof Concrete.ModuleCallExpression) {
      Abstract.Definition module = resolveModuleCall(currentScope, (Concrete.ModuleCallExpression) reference.getExpression());
      if (module != null) {
        Namespace moduleNamespace = nsProviders.statics.forReferable(module);
        return moduleNamespace.resolveName(reference.getName());
      }
      return null;
    } else {
      return null;
    }
  }

  public Abstract.ClassDefinition resolveModuleCall(final Scope currentScope, final Concrete.ModuleCallExpression moduleCall) {
    if (moduleCall.getModule() != null) {
      if (!(moduleCall.getModule() instanceof Abstract.ClassDefinition)) throw new IllegalStateException();
      return (Abstract.ClassDefinition) moduleCall.getModule();
    }

    ModuleNamespace ns = resolveModuleNamespace(moduleCall.getPath());
    return ns == null ? null : ns.getRegisteredClass();
  }

  public Abstract.ClassField resolveClassField(Abstract.ClassDefinition classDefinition, String name) {
    Abstract.Definition resolvedRef = nsProviders.dynamics.forClass(classDefinition).resolveName(name);
    if (resolvedRef instanceof Abstract.ClassField) {
      return (Abstract.ClassField) resolvedRef;
    } else {
      return null;
    }
  }

  public Abstract.ClassField resolveClassFieldByView(Abstract.ClassView classView, String name) {
    if (name.equals(classView.getClassifyingFieldName())) {
      return classView.getClassifyingField();
    }
    for (Abstract.ClassViewField viewField : classView.getFields()) {
      if (name.equals(viewField.getName())) {
        return viewField.getUnderlyingField();
      }
    }
    return null;
  }

}
