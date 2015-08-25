package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;

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
  public int read(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return 0;
  }

  @Override
  public void write(Namespace namespace, ClassDefinition classDefinition) throws IOException {

  }
}
