package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Group;

public interface ModuleResolver {
  Group load(ModulePath modulePath);
}
