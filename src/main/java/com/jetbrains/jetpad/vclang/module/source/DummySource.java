package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public class DummySource implements Source {
  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    return false;
  }
}
