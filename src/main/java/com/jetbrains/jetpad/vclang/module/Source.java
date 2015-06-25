package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  Abstract.ClassDefinition load(ClassDefinition classDefinition) throws IOException;
}
