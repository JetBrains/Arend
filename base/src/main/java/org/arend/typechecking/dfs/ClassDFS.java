package org.arend.typechecking.dfs;

import org.arend.core.definition.ClassDefinition;

public class ClassDFS extends DFS<ClassDefinition,Void> {
  @Override
  protected Void forDependencies(ClassDefinition classDef) {
    for (ClassDefinition superClass : classDef.getSuperClasses()) {
      visit(superClass);
    }
    return null;
  }
}
