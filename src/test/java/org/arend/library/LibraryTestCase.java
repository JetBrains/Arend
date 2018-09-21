package org.arend.library;

import org.arend.ArendTestCase;
import org.arend.prelude.Prelude;
import org.arend.prelude.PreludeLibrary;
import org.junit.Before;

public class LibraryTestCase extends ArendTestCase {
  protected final MemoryLibrary library = new MemoryLibrary(typecheckerState);

  @Before
  public void initialize() {
    libraryManager.setModuleScopeProvider(module -> module.equals(Prelude.MODULE_PATH) ? PreludeLibrary.getPreludeScope() : library.getModuleScopeProvider().forModule(module));
  }
}
