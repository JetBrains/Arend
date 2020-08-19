package org.arend.ext.core.body;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public interface CorePattern extends PrettyPrintable {
  /**
   * If the pattern is a variable pattern, return the binding corresponding to the variable.
   * Otherwise, returns null.
   *
   * @return the variable bound in the pattern or null.
   */
  @Nullable CoreBinding getBinding();

  /**
   * If the pattern is a constructor pattern, returns either a {@link org.arend.ext.core.definition.CoreConstructor}
   * or a {@link org.arend.ext.core.definition.CoreFunctionDefinition} (for defined constructors).
   * Otherwise, returns null.
   *
   * @return the head constructor of the pattern or null.
   */
  @Nullable CoreDefinition getDefinition();

  /**
   * If the pattern is a constructor pattern or a tuple pattern, returns the list of subpatterns.
   * Otherwise, returns the empty list.
   *
   * @return the list of subpatterns.
   */
  @NotNull List<? extends CorePattern> getSubPatterns();

  /**
   * @return true if the pattern is the absurd pattern, false otherwise.
   */
  boolean isAbsurd();

  @Override
  default void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
    DocStringBuilder.build(builder, prettyPrint(ppConfig));
  }

  default LineDoc prettyPrint(PrettyPrinterConfig ppConfig) {
    if (isAbsurd()) {
      return text("()");
    }

    CoreBinding binding = getBinding();
    if (binding != null) {
      return text(binding.getName());
    }

    CoreDefinition definition = getDefinition();
    List<LineDoc> docs = new ArrayList<>();
    if (definition != null) {
      docs.add(refDoc(definition.getRef()));
    }
    for (CorePattern subPattern : getSubPatterns()) {
      docs.add(parens(subPattern.prettyPrint(ppConfig), subPattern.getDefinition() != null));
    }
    return parens(hSep(text(definition == null ? ", " : " "), docs), definition == null);
  }
}
