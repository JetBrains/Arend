package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

import java.io.IOException;

public class ContainerSource implements Source {
  private final ResolvedName myModule;

  public ContainerSource(ResolvedName module) {
    myModule = module;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public long lastModified() {
    return Long.MAX_VALUE;
  }

  @Override
  public boolean isContainer() {
    return true;
  }

  @Override
  public ModuleLoadingResult load() throws IOException {
    return null; // TODO
  }
}
