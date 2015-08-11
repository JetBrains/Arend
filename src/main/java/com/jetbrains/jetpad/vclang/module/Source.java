package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Namespace;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException;
}
