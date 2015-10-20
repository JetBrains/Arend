package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;

import java.io.IOException;
import java.util.List;

public class DummyOutput implements Output {
  @Override
  public boolean canRead() {
    return false;
  }

  @Override
  public boolean canWrite() {
    return false;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public List<List<String>> getDependencies() {
    return null;
  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return null;
  }

  @Override
  public void write() throws IOException {

  }
}
