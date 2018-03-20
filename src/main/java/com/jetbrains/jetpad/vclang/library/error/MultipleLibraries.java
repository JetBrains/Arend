package com.jetbrains.jetpad.vclang.library.error;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.vList;

public class MultipleLibraries extends GeneralError {
  public List<String> libraries;

  public MultipleLibraries(List<String> libraries) {
    super(Level.ERROR, "The following libraries have the same name. The first one will be used.");
    this.libraries = libraries;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return vList(libraries.stream().map(DocFactory::text).collect(Collectors.toList()));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return Collections.emptyList();
  }
}
