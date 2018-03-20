package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;

/**
 * Represents a module persisted in a binary format.
 */
public interface BinarySource extends Source {
  /**
   * Loads the list of hereditary dependencies of this modules and checks that they are available.
   *
   * @param sourceLoader    the state of the loading process.
   *
   * @return true if all dependencies are available, false otherwise.
   */
  boolean loadDependencyInfo(SourceLoader sourceLoader);

  /**
   * Persists the source.
   *
   * @param library         the library to which this source belongs.
   * @param errorReporter   a reporter for all errors that occur during persisting process.
   *
   * @return true if the operation is successful, false otherwise
   */
  boolean persist(SourceLibrary library, ErrorReporter errorReporter);
}
