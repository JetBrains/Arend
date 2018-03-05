package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.library.SourceLibrary;
import com.jetbrains.jetpad.vclang.source.PersistableSource;
import com.jetbrains.jetpad.vclang.source.Source;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;

import java.nio.file.Paths;

public class PreludeBinaryGenerator {
  public static void main(String[] args) {
    TypecheckerState typecheckerState = new SimpleTypecheckerState();
    PreludeFileLibrary library = new PreludeFileLibrary(Paths.get(args[0]), typecheckerState);
    PersistableSource binarySource = library.getBinarySource(Prelude.MODULE_PATH);
    assert binarySource != null;

    if (args.length >= 2 && args[1].equals("--recompile")) {
      library.addFlag(SourceLibrary.Flag.RECOMPILE);
    } else {
      Source rawSource = library.getRawSource(Prelude.MODULE_PATH);
      assert rawSource != null;
      if (rawSource.getTimeStamp() < binarySource.getTimeStamp()) {
        System.out.println("Prelude is up to date");
        return;
      }
    }

    LibraryManager manager = new LibraryManager(name -> { throw new IllegalStateException(); }, library.getModuleScopeProvider(), System.err::println);
    if (manager.loadLibrary(library)) {
      library.typecheck(new Prelude.PreludeTypechecking(typecheckerState), System.err::println);
    }
  }
}
