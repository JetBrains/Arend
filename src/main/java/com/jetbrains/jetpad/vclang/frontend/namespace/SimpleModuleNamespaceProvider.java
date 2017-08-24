package com.jetbrains.jetpad.vclang.frontend.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import java.util.HashMap;

public class SimpleModuleNamespaceProvider extends BaseModuleNamespaceProvider implements ModuleRegistry {
  private final HashMap<GlobalReferable, ModuleNamespace> myMap = new HashMap<>();

  @Override
  public ModuleNamespace forReferable(GlobalReferable referable) {
    return myMap.get(referable);
  }

  @Override
  public ModuleNamespace registerModule(ModulePath modulePath, Group group) {
    SimpleModuleNamespace ns = registerModuleNs(modulePath, group);
    ns.registerClass(group);
    return ns;
  }

  @Override
  public void unregisterModule(ModulePath path) {
    SimpleModuleNamespace ns = ensureModuleNamespace(root(), path);
    ns.unregisterClass();
  }

  private SimpleModuleNamespace registerModuleNs(ModulePath modulePath, Group group) {
    if (myMap.get(group.getReferable()) != null) throw new IllegalStateException();
    SimpleModuleNamespace ns = ensureModuleNamespace(root(), modulePath);
    myMap.put(group.getReferable(), ns);
    return ns;
  }
}
