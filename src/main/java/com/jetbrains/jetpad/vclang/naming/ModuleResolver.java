package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ModuleResolver {
  Abstract.ClassDefinition load(ModulePath modulePath);
}
