package org.arend.typechecking.error;

import org.arend.core.definition.Definition;
import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.arend.error.doc.DocFactory.*;

public class CycleError extends GeneralError {
  public final List<? extends GlobalReferable> cycle;
  public final Concrete.SourceNode cause;

  public CycleError(List<? extends GlobalReferable> cycle, Concrete.SourceNode cause) {
    super(Level.ERROR, "Dependency cycle");
    this.cycle = cycle;
    this.cause = cause;
  }

  public CycleError(List<? extends GlobalReferable> cycle) {
    this(cycle, null);
  }

  public static CycleError fromConcrete(List<? extends Concrete.Definition> cycle) {
    List<GlobalReferable> refs = new ArrayList<>(cycle.size());
    for (Concrete.Definition definition : cycle) {
      refs.add(definition.getData());
    }
    return new CycleError(refs, null);
  }

  public static CycleError fromTypechecked(List<? extends Definition> cycle, Concrete.SourceNode cause) {
    List<GlobalReferable> refs = new ArrayList<>(cycle.size());
    for (Definition definition : cycle) {
      refs.add(definition.getReferable());
    }
    return new CycleError(refs, cause);
  }

  @Override
  public Object getCause() {
    return cause != null ? cause.getData() : cycle.get(0);
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterConfig src) {
    return cause == null ? refDoc(cycle.get(0)) : cause.getData() instanceof Referable ? refDoc((Referable) cause.getData()) : DocFactory.ppDoc(cause, src);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    List<LineDoc> docs = new ArrayList<>(cycle.size() + 1);
    for (GlobalReferable definition : cycle) {
      docs.add(refDoc(definition));
    }
    docs.add(refDoc(cycle.get(0)));
    return hSep(text(" - "), docs);
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return cause != null && cause.getData() instanceof GlobalReferable ? Collections.singletonList((GlobalReferable) cause.getData()) : cycle;
  }

  @Override
  public boolean isTypecheckingError() {
    return true;
  }
}
