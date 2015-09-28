package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;

import java.io.IOException;

public interface Output {
  boolean canRead();
  boolean canWrite();
  long lastModified();
  ModuleLoadingResult read() throws IOException;
  void write(Namespace namespace, ClassDefinition classDefinition) throws IOException;
}
