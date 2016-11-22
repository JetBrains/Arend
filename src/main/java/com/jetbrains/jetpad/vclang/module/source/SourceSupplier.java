package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public interface SourceSupplier<SourceIdT extends SourceId> {
  SourceIdT locateModule(ModulePath modulePath);
  Result loadSource(SourceIdT moduleSourceId, ErrorReporter errorReporter) throws IOException;

  class Result {
    public final byte[] etag;
    public final Abstract.ClassDefinition definition;

    public Result(byte[] etag, Abstract.ClassDefinition definition) {
      this.etag = etag;
      this.definition = definition;
    }
  }
}
