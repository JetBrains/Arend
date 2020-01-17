package org.arend.library.error;

import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;

import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.text;
import static org.arend.ext.prettyprinting.doc.DocFactory.vList;

public class MultipleLibraries extends GeneralError {
  public List<String> libraries;
  public String name;

  public MultipleLibraries(String name, List<String> libraries) {
    super(Level.ERROR, "The following libraries have the same name. The first one will be used.");
    this.name = name;
    this.libraries = libraries;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return text("There are several libraries named '" + name + "'");
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    return vList(libraries.stream().map(DocFactory::text).collect(Collectors.toList()));
  }
}
