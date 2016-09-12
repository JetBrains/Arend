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
    SimpleNamespace ns = new SimpleNamespace();
    forFunction(def, ns);
    return ns;
  }

  public static SimpleNamespace forData(Abstract.DataDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    forData(def, ns);
    return ns;
  }

  public static SimpleNamespace forClass(Abstract.ClassDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    forClass(def, ns);
    return ns;
  }

  private static boolean forFunction(Abstract.FunctionDefinition def, SimpleNamespace ns) {
    return forStatements(def.getStatements(), ns);
  }

  private static boolean forData(Abstract.DataDefinition def, SimpleNamespace ns) {
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
    return true;
  }

  private static boolean forClass(Abstract.ClassDefinition def, SimpleNamespace ns) {
    return forStatements(def.getStatements(), ns);
  }

  private static boolean forClassView(Abstract.ClassView def, SimpleNamespace ns) {
    boolean ok = true;

    for (Abstract.ClassViewField field : def.getFields()) {
      if (field.getUnderlyingField() == null) {
        ok = false;
      } else {
        ns.addDefinition(field);
      }
    }

    return ok;
  }

  private static boolean forStatements(Collection<? extends Abstract.Statement> statements, SimpleNamespace ns) {
    boolean ok = true;
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (STATIC.equals(defst.getStaticMod())) {
        if (defst.getDefinition() instanceof Abstract.ClassView) {
          if (((Abstract.ClassView) defst.getDefinition()).getUnderlyingClass() != null) {
            ns.addDefinition(defst.getDefinition());
          } else {
            ok = false;
          }
          ok = forClassView((Abstract.ClassView) defst.getDefinition(), ns) && ok;
        } else {
          ns.addDefinition(defst.getDefinition());
          if (defst.getDefinition() instanceof Abstract.DataDefinition) {
            ok = forData((Abstract.DataDefinition) defst.getDefinition(), ns) && ok;  // constructors
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
      return forData(def, ns);
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
    public Boolean visitImplement(Abstract.ImplementDefinition def, SimpleNamespace ns) {
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
  }
}
