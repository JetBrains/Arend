package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.STATIC;

public class SimpleStaticNamespaceProvider implements StaticNamespaceProvider {
  public static final StaticNamespaceProvider INSTANCE = new SimpleStaticNamespaceProvider();

  private final Map<Abstract.Definition, Namespace> cache = new HashMap<>();

  public static SimpleNamespace forFunction(Abstract.FunctionDefinition def) {
    return forStatements(def.getStatements());
  }

  public static SimpleNamespace forData(Abstract.DataDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
    return ns;
  }

  public static SimpleNamespace forClass(Abstract.ClassDefinition def) {
    return forStatements(def.getStatements());
  }

  private static SimpleNamespace forClassImplicit(Abstract.ClassDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();

    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      Abstract.ClassDefinition superDef = Abstract.getUnderlyingClassDef(superClass.getSuperClass());
      if (superDef != null) {
        ns.addAll(forClassImplicit(superDef));
      }
    }

    for (Abstract.Statement statement : def.getStatements()) {
      if (statement instanceof Abstract.DefineStatement && ((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.AbstractDefinition) {
        Abstract.AbstractDefinition field = (Abstract.AbstractDefinition) ((Abstract.DefineStatement) statement).getDefinition();
        if (field.isImplicit()) {
          ns.addDefinition(field);
          // Abstract.ClassDefinition classDef = Abstract.getUnderlyingClassDef(field.getResultType());
          // if (classDef != null) {
          //   ns.addAll(forClassImplicit(classDef));
          // }
        }
      }
    }

    return ns;
  }

  private static SimpleNamespace forStatements(Collection<? extends Abstract.Statement> statements) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (STATIC.equals(defst.getStaticMod())) {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          ns.addAll(forData((Abstract.DataDefinition) defst.getDefinition()));  // constructors
        }
        if (defst.getDefinition() instanceof Abstract.ClassDefinition) {
          ns.addAll(forClassImplicit((Abstract.ClassDefinition) defst.getDefinition()));
        }
      }
    }
    return ns;
  }

  @Override
  public Namespace forDefinition(Abstract.Definition definition) {
    Namespace ns = cache.get(definition);
    if (ns != null) return ns;

    ns = definition.accept(DefinitionGetNamespaceVisitor.INSTANCE, null);
    cache.put(definition, ns);
    return ns;
  }

  private static class DefinitionGetNamespaceVisitor implements AbstractDefinitionVisitor<Void, Namespace> {
    public static final DefinitionGetNamespaceVisitor INSTANCE = new DefinitionGetNamespaceVisitor();

    @Override
    public Namespace visitFunction(Abstract.FunctionDefinition def, Void params) {
      return forFunction(def);
    }

    @Override
    public Namespace visitAbstract(Abstract.AbstractDefinition def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitData(Abstract.DataDefinition def, Void params) {
      return forData(def);
    }

    @Override
    public Namespace visitConstructor(Abstract.Constructor def, Void params) {
      return new EmptyNamespace();
    }

    @Override
    public Namespace visitClass(Abstract.ClassDefinition def, Void params) {
      return forClass(def);
    }

    @Override
    public Namespace visitImplement(Abstract.ImplementDefinition def, Void params) {
      return new EmptyNamespace();
    }
  }
}
