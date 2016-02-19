package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;

import java.io.IOException;
import java.util.List;

public interface Output {
  class Header {
    public final List<ModuleID> dependencies;

    public Header(List<ModuleID> dependencies) {
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