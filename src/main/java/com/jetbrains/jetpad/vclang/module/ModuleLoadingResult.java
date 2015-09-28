package com.jetbrains.jetpad.vclang.module;

public class ModuleLoadingResult {
  public Namespace namespace;
  public DefinitionPair definition;
  public boolean compiled;
  public int errorsNumber;

  public ModuleLoadingResult(Namespace namespace, DefinitionPair definition, boolean compiled, int errorsNumber) {
    this.definition = definition;
    this.compiled = compiled;
    this.namespace = namespace;
    this.errorsNumber = errorsNumber;
  }
}
