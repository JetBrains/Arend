package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;

public class SimpleModuleNamespaceProvider extends BaseModuleNamespaceProvider {
  private final HashMap<Abstract.ClassDefinition, ModuleNamespace> myMap = new HashMap<>();

  @Override
  public ModuleNamespace forModule(Abstract.ClassDefinition definition) {
    return myMap.get(definition);
  }

  public ModuleNamespace registerModule(ModulePath modulePath, Abstract.ClassDefinition module) {
    ModuleNamespace ns = registerModuleNs(modulePath, module);
    ns.registerClass(module);
    return ns;
  }

  private ModuleNamespace registerModuleNs(ModulePath modulePath, Abstract.ClassDefinition module) {
    if (myMap.get(module) != null) throw new IllegalStateException();
    ModuleNamespace ns = ensureModuleNamespace(root(), modulePath);
    myMap.put(module, ns);
    return ns;
  }
}
