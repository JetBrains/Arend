package com.jetbrains.jetpad.vclang.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;

/**
 * Represents a module persisted in a binary format.
 */
public interface BinarySource extends Source {
  /**
   * Loads the structure of the source and its dependencies without filling in actual data.
   *
   * @param sourceLoader    the state of the loading process.
   *
   * @return true if all dependencies are available, false otherwise.
   */
  boolean preload(SourceLoader sourceLoader);

  /**
   * Persists the source.
   *
   * @param library             the library to which this source belongs.
   * @param referableConverter  a referable converter.
   * @param errorReporter       a reporter for all errors that occur during persisting process.
   *
   * @return true if the operation is successful, false otherwise
   */
  boolean persist(SourceLibrary library, ReferableConverter referableConverter, ErrorReporter errorReporter);
}
