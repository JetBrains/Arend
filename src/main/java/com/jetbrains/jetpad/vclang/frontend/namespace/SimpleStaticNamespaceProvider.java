package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  private final Map<Abstract.Definition, Namespace> cache = new HashMap<>();

  public static SimpleNamespace forClass(Abstract.ClassDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    forClass(def, ns);
    return ns;
  }

  private static boolean forFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
    return forStatements(def.getStatements(), ns);
  }

  private static void forData(Abstract.DataDefinition def, SimpleNamespace ns) {
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
  }

  private static boolean forClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
    return forStatements(def.getGlobalStatements(), ns);
  }

  private static void forClassView(Abstract.ClassView def, SimpleNamespace ns) {
    for (Abstract.ClassViewField field : def.getFields()) {
      ns.addDefinition(field);
    }
  }

  private static boolean forStatements(Collection<? extends Abstract.Statement> statements, SimpleNamespace ns) {
    boolean ok = true;
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (defst.getDefinition() instanceof Abstract.ClassView) {
        ns.addDefinition(defst.getDefinition());
        forClassView((Abstract.ClassView) defst.getDefinition(), ns);
      } else {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          forData((Abstract.DataDefinition) defst.getDefinition(), ns);  // constructors
        }
        if (defst.getDefinition() instanceof Abstract.ClassViewInstance) {
          Abstract.ClassViewInstance instance = (Abstract.ClassViewInstance) defst.getDefinition();
          if (instance.getClassView().getReferent() instanceof Abstract.ClassView && instance.getClassifyingDefinition() != null) {
            ns.addInstance(instance);
          } else {
            ok = false;
          }
        }
      }
    }
    return ok;
  }

  @Override
  public Namespace forDefinition(Abstract.Definition definition) {
    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    SimpleNamespace sns = new SimpleNamespace();
    if (definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, sns)) {
      cache.put(definition, sns);
    }
    return sns;
  }

  private static class DefinitionGetNamespaceVisitor implements AbstractDefinitionVisitor<SimpleNamespace, Boolean> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Boolean visitFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
      return forFunction(def, ns);
    }

    @Override
    public Boolean visitClassField(Abstract.ClassField def, SimpleNamespace ns) {
      return true;
    }

    @Override
    public Boolean visitData(Abstract.DataDefinition def, SimpleNamespace ns) {
      forData(def, ns);
      return true;
    }

    @Override
    public Boolean visitConstructor(Abstract.Constructor def, SimpleNamespace ns) {
      return true;
    }

    @Override
    public Boolean visitClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
      return forClass(def, ns);
    }

    @Override
    public Boolean visitImplement(Abstract.Implementation def, SimpleNamespace ns) {
      return true;
    }

    @Override
    public Boolean visitClassView(Abstract.ClassView def, SimpleNamespace ns) {
      return true;
    }

    @Override
    public Boolean visitClassViewField(Abstract.ClassViewField def, SimpleNamespace params) {
      return true;
    }

    @Override
    public Boolean visitClassViewInstance(Abstract.ClassViewInstance def, SimpleNamespace params) {
      return true;
    }
  }
}
