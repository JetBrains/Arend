package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  ModuleLoader.Result load() throws IOException;
}
