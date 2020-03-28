package org.arend.library;

import org.arend.ArendTestCase;
import org.junit.Before;

public class LibraryTestCase extends ArendTestCase {
  protected final MemoryLibrary library = new MemoryLibrary(typecheckerState);

  @Before
  public void initialize() {
    setModuleScopeProvider(library.getModuleScopeProvider());
  }
}
