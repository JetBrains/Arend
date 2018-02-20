package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.caching.serialization.DeserializationException;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingException;
import com.jetbrains.jetpad.vclang.module.source.SourceId;

import java.io.IOException;

public class CacheLoadingException extends ModuleLoadingException {
  public CacheLoadingException(SourceId module, String message) {
    super(module, message);
  }

  public CacheLoadingException(SourceId module, DeserializationException deserializationException) {
    this(module, "Corrupted cache: " + deserializationException.getMessage());
  }

  public CacheLoadingException(SourceId module, IOException ioError) {
    super(module, ioError);
  }

  @Override
  public String toString() {
    return sourceId + "(cache): " + getMessage();
  }
}
