package com.jetbrains.jetpad.vclang.oneshot;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class OneshotSourceInfoProvider implements SourceInfoProvider {
  private final Map<Abstract.Definition, ModuleID> modules = new HashMap<>();
  private final Map<Abstract.Definition, FullName> names = new HashMap<>();

  void registerDefinition(Abstract.Definition def, FullName name, ModuleID module) {
    modules.put(def, module);
    names.put(def, name);
  }

  @Override
  public String nameFor(Abstract.Definition definition) {
    FullName name = names.get(definition);
    return name != null ? name.toString() : null;
  }

  @Override
  public ModuleID moduleOf(Abstract.Definition definition) {
    return modules.get(definition);
  }
}
