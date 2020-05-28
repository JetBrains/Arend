package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreDataCallExpression extends CoreExpression {
  @NotNull CoreDataDefinition getDefinition();
  @NotNull CoreSort getSortArgument();
  @NotNull List<? extends CoreExpression> getDefCallArguments();

  /**
   * @return the list of constructors matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<CoreConstructor> computeMatchedConstructors();

  class ConstructorWithDataArguments {
    public final CoreConstructor constructor;
    public final List<? extends CoreExpression> dataTypeArguments;

    public ConstructorWithDataArguments(CoreConstructor constructor, List<? extends CoreExpression> dataTypeArguments) {
      this.constructor = constructor;
      this.dataTypeArguments = dataTypeArguments;
    }
  }

  /**
   * @return the list of constructors with data arguments matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<ConstructorWithDataArguments> computeMatchedConstructorsWithDataArguments();

  /**
   * @return parameters of the given constructor with substituted arguments
   */
  @NotNull CoreParameter getConstructorParameters(CoreConstructor constructor);
}
