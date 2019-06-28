package org.arend.library.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.vList;

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
}
