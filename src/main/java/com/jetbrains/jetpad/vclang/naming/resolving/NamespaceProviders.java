package com.jetbrains.jetpad.vclang.naming.resolving;

import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;

public class NamespaceProviders {
  public final ModuleNamespaceProvider modules;
  public final StaticNamespaceProvider statics;
  public final DynamicNamespaceProvider dynamics;

  public NamespaceProviders(ModuleNamespaceProvider modules, StaticNamespaceProvider statics, DynamicNamespaceProvider dynamics) {
    this.modules = modules;
    this.statics = statics;
    this.dynamics = dynamics;
  }
}
