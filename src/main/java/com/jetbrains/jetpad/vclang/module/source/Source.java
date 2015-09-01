package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException;
}
