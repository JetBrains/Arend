package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.ext.core.level.CoreLevels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreDataCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreDataDefinition getDefinition();
  @NotNull CoreLevels getLevels();

  /**
   * Computes the list of constructors matching this type.
   *
   * @return true if the list can be determined, false otherwise.
   */
  boolean computeMatchedConstructors(List<? super CoreConstructor> result);

  /**
   * @return the list of constructors matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<CoreConstructor> computeMatchedConstructors();
}
