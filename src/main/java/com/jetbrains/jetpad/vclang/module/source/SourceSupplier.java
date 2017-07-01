package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface SourceSupplier<SourceIdT extends SourceId> {
  SourceIdT locateModule(@Nonnull ModulePath modulePath);
  boolean isAvailable(@Nonnull SourceIdT sourceId);
  Abstract.ClassDefinition loadSource(@Nonnull SourceIdT sourceId, @Nonnull ErrorReporter errorReporter);
}
