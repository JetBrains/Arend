package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;
import com.jetbrains.jetpad.vclang.naming.FullName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.HashMap;
import java.util.Map;

public class SimpleSourceInfoProvider implements SourceInfoProvider {
  private final Map<Abstract.Definition, ModuleSourceId> modules = new HashMap<>();
  private final Map<Abstract.Definition, FullName> names = new HashMap<>();

  public void registerDefinition(Abstract.Definition def, FullName name, ModuleSourceId source) {
    modules.put(def, source);
    names.put(def, name);
  }

  @Override
  public String nameFor(Abstract.Definition definition) {
    FullName name = names.get(definition);
    return name != null ? name.toString() : null;
  }

  @Override
  public ModuleSourceId sourceOf(Abstract.Definition definition) {
    return modules.get(definition);
  }
}
