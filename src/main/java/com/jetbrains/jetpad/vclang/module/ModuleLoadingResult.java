package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

public class ModuleLoadingResult {
  public ClassDefinition classDefinition;
  public boolean compiled;

  public ModuleLoadingResult(ClassDefinition classDefinition, boolean compiled) {
    this.classDefinition = classDefinition;
    this.compiled = compiled;
  }
}
