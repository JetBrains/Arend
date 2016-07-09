package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod.STATIC;

public class SimpleDynamicNamespaceProvider implements DynamicNamespaceProvider {
  public static final DynamicNamespaceProvider INSTANCE = new SimpleDynamicNamespaceProvider();

  private final Map<Abstract.ClassDefinition, SimpleNamespace> classCache = new HashMap<>();

  @Override
  public SimpleNamespace forClass(final Abstract.ClassDefinition classDefinition) {
    SimpleNamespace ns = classCache.get(classDefinition);
    if (ns != null) return ns;

    ns = forStatements(classDefinition.getStatements());
    for (final Abstract.SuperClass superClass : classDefinition.getSuperClasses()) {
      Abstract.ClassDefinition superDef = getUnderlyingClassDef(superClass.getSuperClass());
      if (superDef == null) continue;

      SimpleNamespace namespace = forClass(superDef);

      Map<String, String> renamings = new HashMap<>();
      for (Abstract.IdPair idPair : superClass.getRenamings()) {
        renamings.put(idPair.getFirstName(), idPair.getSecondName());
      }
      Set<String> hiding = new HashSet<>();
      for (Abstract.Identifier identifier : superClass.getHidings()) {
        hiding.add(identifier.getName());
      }

      for (Map.Entry<String, Referable> entry : namespace.getEntrySet()) {
        if (hiding.contains(entry.getKey())) continue;
        String newName = renamings.get(entry.getKey());
        ns.addDefinition(newName != null ? newName : entry.getKey(), entry.getValue());
      }
    }

    classCache.put(classDefinition, ns);
    return ns;
  }

  private Abstract.ClassDefinition getUnderlyingClassDef(Abstract.Expression expr) {
    if (expr instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) expr).getReferent() instanceof Abstract.ClassDefinition) {
      return (Abstract.ClassDefinition) ((Abstract.DefCallExpression) expr).getReferent();
    } else if (expr instanceof Abstract.ClassExtExpression) {
      return getUnderlyingClassDef(((Abstract.ClassExtExpression) expr).getBaseClassExpression());
    } else {
      return null;
    }
  }

  private static SimpleNamespace forData(Abstract.DataDefinition def) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Constructor constructor : def.getConstructors()) {
      ns.addDefinition(constructor);
    }
    return ns;
  }

  private static SimpleNamespace forStatements(Collection<? extends Abstract.Statement> statements) {
    SimpleNamespace ns = new SimpleNamespace();
    for (Abstract.Statement statement : statements) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!STATIC.equals(defst.getStaticMod())) {
        ns.addDefinition(defst.getDefinition());
        if (defst.getDefinition() instanceof Abstract.DataDefinition) {
          ns.addAll(forData((Abstract.DataDefinition) defst.getDefinition()));  // constructors
        }
      }
    }
    return ns;
  }
}
