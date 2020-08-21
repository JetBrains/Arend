package org.arend.ext.core.body;

import org.arend.ext.core.context.CoreParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreElimBody extends CoreBody {
  @NotNull List<? extends CoreElimClause> getClauses();

  /**
   * Computes a list of pattern sequences that actually need to be matched in order for this body to compute.
   * This function does not compute types of bindings; so, they may be incorrect.
   *
   * @param parameters  parameters of this body (i.e., either {@link org.arend.ext.core.expr.CoreCaseExpression#getParameters} or {@link org.arend.ext.core.definition.CoreFunctionDefinition#getParameters})
   */
  @NotNull List<List<CorePattern>> computeRefinedPatterns(@NotNull CoreParameter parameters);
}
