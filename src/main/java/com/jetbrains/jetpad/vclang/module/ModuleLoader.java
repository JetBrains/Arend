package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleLoader {
  Abstract.ClassDefinition load(ModulePath modulePath);
}
