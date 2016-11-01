package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.util.HashMap;

public class SimpleModuleNamespaceProvider extends BaseModuleNamespaceProvider {
  private final HashMap<Object, ModuleNamespace> myMap = new HashMap<>();

  @Override
  public ModuleNamespace forModule(Abstract.ClassDefinition definition) {
    return myMap.get(definition);
  }

  @Override
  public ModuleNamespace forModule(ClassDefinition definition) {
    return myMap.get(definition);
  }

  public ModuleNamespace registerModule(ModulePath modulePath, Abstract.ClassDefinition module) {
    if (module.getKind() != Abstract.ClassDefinition.Kind.Module) throw new IllegalArgumentException();
    ModuleNamespace ns = registerModuleNs(modulePath, module);
    ns.registerClass(module);
    return ns;
  }

  private ModuleNamespace registerModuleNs(ModulePath modulePath, Object module) {
    if (myMap.get(module) != null) throw new IllegalStateException();
    ModuleNamespace ns = ensureModuleNamespace(root(), modulePath);
    myMap.put(module, ns);
    return ns;
  }
}
