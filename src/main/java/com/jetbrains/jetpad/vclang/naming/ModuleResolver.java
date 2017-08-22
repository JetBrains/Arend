package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface ModuleResolver {
  Concrete.ClassDefinition load(ModulePath modulePath);
}
