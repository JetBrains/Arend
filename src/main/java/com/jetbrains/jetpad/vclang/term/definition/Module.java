package com.jetbrains.jetpad.vclang.term.definition;

public class Module {
  private final Module myParent;
  private final ClassDefinition myClass;

  public Module(Module parent, ClassDefinition classDef) {
    myParent = parent;
    myClass = classDef;
  }

  public Module getParent() {
    return myParent;
  }

  public ClassDefinition getClassDef() {
    return myClass;
  }
}
