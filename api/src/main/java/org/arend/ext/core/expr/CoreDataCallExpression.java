package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreDataCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreDataDefinition getDefinition();

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

  interface ConstructorWithDataArguments {
    @NotNull CoreConstructor getConstructor();
    @NotNull List<? extends CoreExpression> getDataTypeArguments();

    /**
     * @return Parameters of this constructor with substituted data type arguments and sort argument.
     */
    @NotNull CoreParameter getParameters();
  }

  /**
   * Computes the list of constructors with data arguments matching this type.
   *
   * @return true if the list can be determined, false otherwise.
   */
  boolean computeMatchedConstructorsWithDataArguments(List<? super ConstructorWithDataArguments> result);

  /**
   * @return the list of constructors with data arguments matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<ConstructorWithDataArguments> computeMatchedConstructorsWithDataArguments();
}
