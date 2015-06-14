package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

import java.io.File;

public class Module {
  private final ClassDefinition myParent;
  private final String myName;

  public Module(ClassDefinition parent, String name) {
    myParent = parent;
    myName = name;
  }

  private File getFile(File dir, Definition def) {
    return def == ModuleLoader.rootModule() ? dir : new File(getFile(dir, def.getParent()), def.getName());
  }

  public File getFile(File dir, String ext) {
    return new File(getFile(dir, myParent), myName + ext);
  }

  public ClassDefinition getParent() {
    return myParent;
  }

  public String getName() {
    return myName;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Module && myParent == ((Module) other).myParent && myName.equals(((Module) other).myName);
  }

  private String toString(Definition def) {
    return def == ModuleLoader.rootModule() ? "" : toString(def.getParent()) + def.getName() + ".";
  }

  @Override
  public String toString() {
    return toString(myParent) + myName;
  }
}
