package org.arend.frontend;

import org.arend.extImpl.DefinitionRequester;
import org.arend.frontend.library.PreludeFileLibrary;
import org.arend.library.LibraryManager;
import org.arend.library.SourceLibrary;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.prelude.Prelude;
import org.arend.source.BinarySource;
import org.arend.source.Source;
import org.arend.typechecking.instance.provider.InstanceProviderSet;

import java.nio.file.Paths;

public class PreludeBinaryGenerator {
  public static void main(String[] args) {
    PreludeFileLibrary library = new PreludeFileLibrary(Paths.get(args[0]));
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

    LibraryManager manager = new LibraryManager((lib,name) -> { throw new IllegalStateException(); }, new InstanceProviderSet(), System.err::println, System.err::println, DefinitionRequester.INSTANCE, null);
    if (manager.loadLibrary(library, null)) {
      if (new Prelude.PreludeTypechecking(manager.getInstanceProviderSet(), ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, PositionComparator.INSTANCE).typecheckLibrary(library)) {
        library.persistModule(Prelude.MODULE_PATH, IdReferableConverter.INSTANCE, System.err::println);
      }
    }
  }
}
