package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public interface ModuleLoader {
  ModuleLoadingResult load(ResolvedName module, boolean tryLoad);
  void save(ResolvedName module);
  void savingError(GeneralError error);
  void loadingError(GeneralError error);
  void loadingSucceeded(ResolvedName module, NamespaceMember definition, boolean compiled);
}
