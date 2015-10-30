package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  boolean isContainer();
  ModuleLoadingResult load() throws IOException;
}
