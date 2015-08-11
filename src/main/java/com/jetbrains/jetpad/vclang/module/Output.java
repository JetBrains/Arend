package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;

import java.io.IOException;

public interface Output {
  boolean canRead();
  boolean canWrite();
  long lastModified();
  int read(Namespace namespace, ClassDefinition classDefinition) throws IOException;
  void write(Namespace namespace, ClassDefinition classDefinition) throws IOException;
}
