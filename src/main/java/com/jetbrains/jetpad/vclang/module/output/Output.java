package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import java.io.IOException;
import java.util.List;

public interface Output {
  class Header {
    public final List<ModuleID> dependencies;

    public Header(List<ModuleID> dependencies) {
      this.dependencies = dependencies;
    }
  }

  Header getHeader() throws IOException;

  boolean canRead();
  boolean canWrite();
  long lastModified();

  void readStubs() throws IOException;
  ModuleLoader.Result read() throws IOException;
  void write() throws IOException;
}