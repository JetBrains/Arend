package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.io.IOException;

public interface SourceSupplier<SourceIdT extends SourceId> {
  SourceIdT locateModule(ModulePath modulePath);
  boolean isAvailable(SourceIdT sourceId);
  Abstract.ClassDefinition loadSource(SourceIdT sourceId, ErrorReporter errorReporter) throws IOException;
}
