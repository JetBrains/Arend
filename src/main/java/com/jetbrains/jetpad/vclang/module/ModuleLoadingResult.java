package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class ModuleLoadingResult {
  public NamespaceMember namespaceMember;
  public boolean compiled;
  public int errorsNumber;

  public ModuleLoadingResult(NamespaceMember namespaceMember, boolean compiled, int errorsNumber) {
    this.namespaceMember = namespaceMember;
    this.compiled = compiled;
    this.errorsNumber = errorsNumber;
  }
}
