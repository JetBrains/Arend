package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.PersistableSourceLibrary;

/**
 * Represents a module that can be persisted as well as loaded.
 */
public interface PersistableSource extends Source {
  /**
   * Persists the source.
   *
   * @param library       the library to which this source belongs.
   * @param errorReporter a reporter for all errors that occur during persisting process.
   *
   * @return true if the operation is successful, false otherwise
   */
  boolean persist(PersistableSourceLibrary library, ErrorReporter errorReporter);
}
