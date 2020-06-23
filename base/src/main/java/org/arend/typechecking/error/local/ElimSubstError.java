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
  public final Collection<?> notEliminatedBindings; // either Referable or Binding

  public ElimSubstError(Collection<?> notEliminatedBindings, @Nullable ConcreteSourceNode cause) {
    super("", cause);
    this.notEliminatedBindings = notEliminatedBindings;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    List<LineDoc> docs = new ArrayList<>(notEliminatedBindings.size());
    for (Object binding : notEliminatedBindings) {
      docs.add(binding instanceof Referable ? refDoc((Referable) binding) : text(binding instanceof Binding ? ((Binding) binding).getName() : binding.toString()));
    }
    return hList(text("Cannot perform substitution since the following bindings are not eliminated: "), hSep(text(", "), docs));
  }
}
