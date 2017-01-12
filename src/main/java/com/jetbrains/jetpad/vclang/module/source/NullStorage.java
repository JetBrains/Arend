package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NullStorage<SourceIdT extends SourceId> implements Storage<SourceIdT> {
  @Override
  public InputStream getCacheInputStream(SourceIdT sourceId) {
    return null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceIdT sourceId) {
    return null;
  }

  @Override
  public SourceIdT locateModule(ModulePath modulePath) {
    return null;
  }

  @Override
  public boolean isAvailable(SourceIdT sourceId) {
    return false;
  }

  @Override
  public Abstract.ClassDefinition loadSource(SourceIdT sourceId, ErrorReporter errorReporter) throws IOException {
    return null;
  }
}
