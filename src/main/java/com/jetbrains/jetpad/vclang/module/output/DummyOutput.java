package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public class DummyOutput implements Output {
  @Override
  public boolean canRead() {
    return false;
  }

  @Override
  public boolean canWrite() {
    return false;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return null;
  }

  @Override
  public void write(Namespace namespace, ClassDefinition classDefinition) throws IOException {

  }
}
