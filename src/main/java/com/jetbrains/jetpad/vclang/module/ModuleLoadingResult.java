package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

public class ModuleLoadingResult {
  public Namespace namespace;
  public ClassDefinition classDefinition;
  public boolean compiled;
  public int errorsNumber;

  public ModuleLoadingResult(Namespace namespace, ClassDefinition classDefinition, boolean compiled, int errorsNumber) {
    this.classDefinition = classDefinition;
    this.compiled = compiled;
    this.namespace = namespace;
    this.errorsNumber = errorsNumber;
  }
}
