package com.jetbrains.jetpad.vclang.module.error;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

import java.io.IOException;

public class ModuleLoadingException extends Exception {
  public final SourceId sourceId;

  public ModuleLoadingException(SourceId sourceId, String message) {
    super(message);
    this.sourceId = sourceId;
  }

  public ModuleLoadingException(SourceId sourceId, IOException e) {
    super(e);
    this.sourceId = sourceId;
  }
}
