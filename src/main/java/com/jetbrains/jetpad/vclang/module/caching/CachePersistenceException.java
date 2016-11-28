package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

import java.io.IOException;

public class CachePersistenceException extends Exception {
  public final SourceId sourceId;

  public CachePersistenceException(SourceId sourceId, String message) {
    super(message);
    this.sourceId = sourceId;
  }

  public CachePersistenceException(SourceId mySourceId, IOException ioError) {
    super(ioError);
    this.sourceId = mySourceId;
  }
}
