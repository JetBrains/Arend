package org.arend.ext.core.body;

import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.prettyprinting.PrettyPrintable;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  @Nullable CoreDefinition getConstructor();

  /**
   * @return the parameters of the definition or the \Sigma-type
   */
  @NotNull CoreParameter getParameters();

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

  /**
   * @return all bindings in this pattern stitched into a single linked list.
   */
  @NotNull CoreParameter getAllBindings();

  @NotNull CorePattern subst(@NotNull Map<? extends CoreBinding, ? extends CorePattern> map);

  default String getBindingName() {
    CoreBinding binding = getBinding();
    if (binding == null) return null;
    String name = binding.getName();
    return name == null ? "_x" : name;
  }

  @Override
  default void prettyPrint(StringBuilder builder, PrettyPrinterConfig ppConfig) {
    if (isAbsurd()) {
      builder.append("()");
      return;
    }

    CoreBinding binding = getBinding();
    if (binding != null) {
      builder.append(getBindingName());
      return;
    }

    CoreDefinition definition = getConstructor();
    List<LineDoc> docs = new ArrayList<>();
    if (definition != null) {
      docs.add(text(definition.getRef().getRefName()));
    }
    for (CorePattern subPattern : getSubPatterns()) {
      docs.add(parens(subPattern.prettyPrint(ppConfig), subPattern.getConstructor() != null && !subPattern.getSubPatterns().isEmpty()));
    }

    DocStringBuilder.build(builder, parens(hSep(text(definition == null ? ", " : " "), docs), definition == null));
  }

  @Override
  default LineDoc prettyPrint(PrettyPrinterConfig ppConfig) {
    return pattern(this, ppConfig);
  }
}
