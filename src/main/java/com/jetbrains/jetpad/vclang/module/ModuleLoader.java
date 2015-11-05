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

  class Helper {
    static void processLoaded(ModuleLoader moduleLoader, ResolvedName module, ModuleLoadingResult result) {
      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new GeneralError(module.parent.getResolvedName(), result == null ? "cannot load module '" + module.name + "'" : "module '" + module.name + "' contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
        error.setLevel(GeneralError.Level.INFO);
        moduleLoader.loadingError(error);
      } else {
        if (result.namespaceMember != null && (result.namespaceMember.abstractDefinition != null || result.namespaceMember.definition != null)) {
          moduleLoader.loadingSucceeded(module, result.namespaceMember, result.compiled);
        }
      }

      if (result != null && result.namespaceMember != null) {
        module.parent.addMember(result.namespaceMember);
      }
    }
  }
}
