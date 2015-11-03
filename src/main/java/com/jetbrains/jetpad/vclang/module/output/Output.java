package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;

import java.io.IOException;
import java.util.List;

public interface Output {
  class Header {
    public final List<List<String>> dependencies;
    public final List<String> provided;

    public Header(List<List<String>> dependencies, List<String> provided) {
      this.dependencies = dependencies;
      this.provided = provided;
    }
  }

  Header getHeader() throws IOException;

  boolean canRead();
  boolean canWrite();
  long lastModified();

  boolean isContainer();

  void readStubs() throws IOException;
  ModuleLoadingResult read() throws IOException;
  void write() throws IOException;
}
