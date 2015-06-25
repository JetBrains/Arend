package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public interface Output {
  boolean canRead();
  boolean canWrite();
  long lastModified();
  int read(ClassDefinition classDefinition) throws IOException;
  void write(ClassDefinition classDefinition) throws IOException;
}
