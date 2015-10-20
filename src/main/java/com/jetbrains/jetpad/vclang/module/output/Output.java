package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;

import java.io.IOException;
import java.util.List;

public interface Output {
  boolean canRead();
  boolean canWrite();
  long lastModified();
  List<List<String>> getDependencies();
  ModuleLoadingResult read() throws IOException;
  void write() throws IOException;
}
