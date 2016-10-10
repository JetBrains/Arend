package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public interface SourceSupplier<SourceIdT extends SourceId> {
  SourceIdT locateModule(ModulePath modulePath);
  Result loadSource(SourceIdT moduleSourceId) throws IOException;

  class Result {
    public final byte[] etag;
    public final Abstract.ClassDefinition definition;
    public final int errorCount;

    public Result(byte[] etag, Abstract.ClassDefinition definition, int errorCount) {
      this.etag = etag;
      this.definition = definition;
      this.errorCount = errorCount;
    }
  }
}
