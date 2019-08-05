package org.arend.frontend;

import org.arend.frontend.library.PreludeFileLibrary;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.prelude.Prelude;
import org.arend.source.BinarySource;
import org.arend.source.Source;
import org.arend.typechecking.SimpleTypecheckerState;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;

import java.nio.file.Paths;

public class PreludeBinaryGenerator {
  public static void main(String[] args) {
    TypecheckerState typecheckerState = new SimpleTypecheckerState();
    PreludeFileLibrary library = new PreludeFileLibrary(Paths.get(args[0]), typecheckerState);
    BinarySource binarySource = library.getBinarySource(Prelude.MODULE_PATH);
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

    LibraryManager manager = new LibraryManager(name -> { throw new IllegalStateException(); }, new InstanceProviderSet(), System.err::println, System.err::println);
    if (manager.loadLibrary(library)) {
      if (new Prelude.PreludeTypechecking(manager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, PositionComparator.INSTANCE).typecheckLibrary(library)) {
        library.persistModule(Prelude.MODULE_PATH, IdReferableConverter.INSTANCE, System.err::println);
      }
    }
  }
}
