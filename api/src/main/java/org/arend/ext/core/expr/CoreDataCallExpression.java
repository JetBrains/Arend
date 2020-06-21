package org.arend.ext.core.expr;

import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreConstructor;
import org.arend.ext.core.definition.CoreDataDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CoreDataCallExpression extends CoreDefCallExpression {
  @Override @NotNull CoreDataDefinition getDefinition();

  /**
   * @return the list of constructors matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<CoreConstructor> computeMatchedConstructors();

  class ConstructorWithDataArguments {
    public final @NotNull CoreConstructor constructor;
    public final @NotNull List<? extends CoreExpression> dataTypeArguments;

    public ConstructorWithDataArguments(@NotNull CoreConstructor constructor, @NotNull List<? extends CoreExpression> dataTypeArguments) {
      this.constructor = constructor;
      this.dataTypeArguments = dataTypeArguments;
    }
  }

  /**
   * @return the list of constructors with data arguments matching this type or {@code null} if it cannot be determined
   */
  @Nullable List<ConstructorWithDataArguments> computeMatchedConstructorsWithDataArguments();

  class ConstructorWithParameters extends ConstructorWithDataArguments {
    public final @NotNull CoreParameter parameters;

    public ConstructorWithParameters(CoreConstructor constructor, List<? extends CoreExpression> dataTypeArguments, @NotNull CoreParameter parameters) {
      super(constructor, dataTypeArguments);
      this.parameters = parameters;
    }
  }

  /**
   * @return the list of constructors with data arguments matching this type and their parameters with substituted arguments or {@code null} if it cannot be determined
   */
  @Nullable List<ConstructorWithParameters> computeMatchedConstructorsWithParameters();
}
