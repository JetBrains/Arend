package org.arend.typechecking.error;

import org.arend.core.definition.Definition;
import org.arend.error.GeneralError;
import org.arend.error.doc.Doc;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.termination.CompositeCallMatrix;
import org.arend.typechecking.termination.RecursiveBehavior;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class TerminationCheckError extends GeneralError {
  public final GlobalReferable definition;
  public final Set<? extends Definition> definitions;
  public final Set<RecursiveBehavior<Definition>> behaviors;

  public TerminationCheckError(Definition def, Set<? extends Definition> definitions, Set<RecursiveBehavior<Definition>> behaviors) {
    super(Level.ERROR, "");
    definition = def.getReferable();
    this.definitions = definitions;
    this.behaviors = behaviors;
  }

  @Override
  public Object getCause() {
    return definition;
  }

  public Doc getCauseDoc(PrettyPrinterConfig infoProvider) {
    return refDoc(definition);
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("Termination check failed for function '"), refDoc(definition), text("'"));
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(behaviors.stream().map(rb -> printBehavior(rb, ppConfig)).collect(Collectors.toList()));
  }

  private static Doc printBehavior(RecursiveBehavior rb, PrettyPrinterConfig ppConfig) {
    return hang(text(rb.initialCallMatrix instanceof CompositeCallMatrix ? "Problematic sequence of recursive calls:" : "Problematic recursive call:"), rb.initialCallMatrix.getMatrixLabel(ppConfig));
  }

  @Override
  public Collection<? extends GlobalReferable> getAffectedDefinitions() {
    return definitions.stream().map(Definition::getReferable).collect(Collectors.toList());
  }

  @Override
  public boolean isTypecheckingError() {
    return true;
  }
}
