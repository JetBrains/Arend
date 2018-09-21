package org.arend.typechecking.error;

import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class CycleError extends GeneralError {
  public final List<Concrete.Definition> cycle;

  public CycleError(List<Concrete.Definition> cycle) {
    super(Level.ERROR, "Dependency cycle");
    this.cycle = cycle;
  }

  @Override
  public Object getCause() {
    return cycle.get(0).getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return DocFactory.ppDoc(cycle.get(0), src);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    List<LineDoc> docs = new ArrayList<>(cycle.size() + 1);
    docs.add(refDoc(cycle.get(cycle.size() - 1).getData()));
    for (Concrete.Definition definition : cycle) {
      docs.add(refDoc(definition.getData()));
    }
    return hSep(text(" - "), docs);
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return cycle.stream().map(Concrete.ReferableDefinition::getData).collect(Collectors.toList());
  }
}
