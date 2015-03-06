package com.jetbrains.jetpad.vclang.editor;

import com.jetbrains.jetpad.vclang.model.Module;
import jetbrains.jetpad.mapper.Mapper;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forDefinitions;

public class ModuleMapper extends Mapper<Module, ModuleCell> {
  public ModuleMapper(Module source) {
    super(source, new ModuleCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forDefinitions(this, getSource().definitions, getTarget().definitions));
  }
}
