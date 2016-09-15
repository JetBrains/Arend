package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

import java.io.IOException;
import java.util.List;

public interface Output {
  class Header {
    public final List<ModuleSourceId> dependencies;

    public Header(List<ModuleSourceId> dependencies) {
      this.dependencies = dependencies;
    }
  }

  boolean canRead();
  boolean canWrite();
  long lastModified();

  Header readHeader() throws IOException;
  void readStubs() throws IOException;
  ModuleLoader.Result read() throws IOException;

  void write() throws IOException;
}