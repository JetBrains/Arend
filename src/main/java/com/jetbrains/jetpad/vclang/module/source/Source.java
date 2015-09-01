package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;

import java.io.IOException;

public interface Source {
  boolean isAvailable();
  long lastModified();
  ModuleLoadingResult load(Namespace namespace) throws IOException;
}
