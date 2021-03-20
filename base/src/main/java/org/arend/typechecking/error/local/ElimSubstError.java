package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Binding;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ElimSubstError extends TypecheckingError {
  public final Referable referable;
  public final Collection<?> notEliminatedBindings; // either Referable or Binding

  public ElimSubstError(Referable referable, Collection<?> notEliminatedBindings, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.referable = referable;
    this.notEliminatedBindings = notEliminatedBindings;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    List<LineDoc> docs = new ArrayList<>(notEliminatedBindings.size());
    for (Object binding : notEliminatedBindings) {
      docs.add(binding instanceof Referable ? refDoc((Referable) binding) : text(binding instanceof Binding ? ((Binding) binding).getName() : binding.toString()));
    }
    return referable == null
      ? hList(text("Cannot perform substitution since " + (docs.size() > 1 ? "bindings " : "binding ")), hSep(text(", "), docs), text((docs.size() > 1 ? " are" : " is") + " not eliminated"))
      : hList(text(docs.size() > 1 ? "Bindings " : "Binding "), hSep(text(", "), docs), text(" should be eliminated after "), refDoc(referable));
  }
}
