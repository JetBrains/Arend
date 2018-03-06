package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.VclangTestCase;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.prelude.PreludeLibrary;
import org.junit.Before;

public class LibraryTestCase extends VclangTestCase {
  protected final MemoryLibrary library = new MemoryLibrary(typecheckerState);

  @Before
  public void initialize() {
    libraryManager.setModuleScopeProvider(module -> module.equals(Prelude.MODULE_PATH) ? PreludeLibrary.getPreludeScope() : library.getModuleScopeProvider().forModule(module));
  }
}
