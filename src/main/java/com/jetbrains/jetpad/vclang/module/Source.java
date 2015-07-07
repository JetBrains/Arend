package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  boolean load(ClassDefinition classDefinition) throws IOException;
}
