package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.module.serialization.DefinitionContextProvider;

/**
 * Represents a module that can be persisted as well as loaded.
 */
public interface PersistableSource extends Source {
  /**
   * Persists the source.
   *
   * @param library         the library to which this source belongs.
   * @param contextProvider a context provider.
   * @param errorReporter   a reporter for all errors that occur during persisting process.
   *
   * @return true if the operation is successful, false otherwise
   */
  boolean persist(SourceLibrary library, DefinitionContextProvider contextProvider, ErrorReporter errorReporter);
}
