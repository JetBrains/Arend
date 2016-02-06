package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;

import java.io.IOException;

public class DummySource implements Source {
  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public ModuleLoader.Result load() throws IOException {
    return null;
  }
}
