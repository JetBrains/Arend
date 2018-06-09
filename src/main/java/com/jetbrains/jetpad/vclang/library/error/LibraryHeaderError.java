package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.error.SourcePosition;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import java.util.Collection;
import java.util.Collections;

public class LibraryHeaderError extends GeneralError {
  private final SourceInfo errorPosition;

  public LibraryHeaderError(String libraryName, String message) {
    super(Level.ERROR, message);
    errorPosition = null;
  }

  public LibraryHeaderError(String libraryName, int line, int column, String message) {
    super(Level.ERROR, message);
    errorPosition = new SourcePosition(libraryName, line, column);
  }

  @Override
  public Object getCause() {
    return errorPosition;
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
