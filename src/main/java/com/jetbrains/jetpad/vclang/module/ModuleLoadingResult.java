package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

public class ModuleLoadingResult {
  public Namespace namespace;
  public ClassDefinition classDefinition;
  public boolean compiled;

  public ModuleLoadingResult(Namespace namespace, ClassDefinition classDefinition, boolean compiled) {
    this.classDefinition = classDefinition;
    this.compiled = compiled;
    this.namespace = namespace;
  }
}
