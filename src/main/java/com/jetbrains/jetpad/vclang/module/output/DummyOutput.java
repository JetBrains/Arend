package com.jetbrains.jetpad.vclang.module.output;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;

import java.io.IOException;

public class DummyOutput implements Output {
  @Override
  public Header getHeader() throws IOException {
    return null;
  }

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
  public void readStubs() throws IOException {

  }

  @Override
  public ModuleLoadingResult read() throws IOException {
    return null;
  }

  @Override
  public void write() throws IOException {

  }
}
