package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.HashMap;

public class SimpleModuleNamespaceProvider extends BaseModuleNamespaceProvider implements ModuleRegistry {
  private final HashMap<Concrete.ClassDefinition, ModuleNamespace> myMap = new HashMap<>();

  @Override
  public ModuleNamespace forModule(Concrete.ClassDefinition definition) {
    return myMap.get(definition);
  }

  @Override
  public ModuleNamespace registerModule(ModulePath modulePath, Concrete.ClassDefinition module) {
    SimpleModuleNamespace ns = registerModuleNs(modulePath, module);
    ns.registerClass(module);
    return ns;
  }

  @Override
  public void unregisterModule(ModulePath path) {
    SimpleModuleNamespace ns = ensureModuleNamespace(root(), path);
    ns.unregisterClass();
  }

  private SimpleModuleNamespace registerModuleNs(ModulePath modulePath, Concrete.ClassDefinition module) {
    if (myMap.get(module) != null) throw new IllegalStateException();
    SimpleModuleNamespace ns = ensureModuleNamespace(root(), modulePath);
    myMap.put(module, ns);
    return ns;
  }
}
