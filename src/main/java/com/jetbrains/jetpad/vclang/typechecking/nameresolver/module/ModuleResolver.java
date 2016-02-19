package com.jetbrains.jetpad.vclang.typechecking.nameresolver.module;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;

public interface ModuleResolver {
  NamespaceMember locateModule(ModulePath modulePath);
}
