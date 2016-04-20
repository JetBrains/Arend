package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.HashSet;
import java.util.Set;

public class ClassNamespace implements Namespace {
  private final Abstract.ClassDefinition myClass;

  public ClassNamespace(Abstract.ClassDefinition cls) {
    this.myClass = cls;
  }

  @Override
  public Set<String> getNames() {
    // FIXME[where]
    Set<String> names = new HashSet<>();
    for (Abstract.Statement statement : myClass.getStatements()) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!StaticMod.DYNAMIC.equals(defst.getStaticMod())) {
        names.add(((Abstract.DefineStatement) statement).getDefinition().getName());
      }
    }
    return names;
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    // FIXME[where]
    for (Abstract.Statement statement : myClass.getStatements()) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!StaticMod.DYNAMIC.equals(defst.getStaticMod()) && name.equals(defst.getDefinition().getName())) {
        return defst.getDefinition();
      }
    }
    return null;
  }

  @Override
  public Referable resolveInstanceName(String name) {
    // FIXME[where]
    for (Abstract.Statement statement : myClass.getStatements()) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (StaticMod.DYNAMIC.equals(defst.getStaticMod()) && name.equals(defst.getDefinition().getName())) {
        return defst.getDefinition();
      }
    }
    return null;
  }
}
