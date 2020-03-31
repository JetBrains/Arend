package org.arend.extImpl;

import org.arend.ext.concrete.ConcreteAppBuilder;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConcreteAppBuilderImpl implements ConcreteAppBuilder {
  private final Object myData;
  private final Concrete.Expression myFunction;
  private final List<Concrete.Argument> myArguments;

  public ConcreteAppBuilderImpl(Object data, Concrete.Expression function) {
    myData = data;
    if (function instanceof Concrete.AppExpression) {
      myFunction = ((Concrete.AppExpression) function).getFunction();
      myArguments = new ArrayList<>(((Concrete.AppExpression) function).getArguments());
    } else {
      myFunction = function;
      myArguments = new ArrayList<>();
    }
  }

  @Override
  public @NotNull ConcreteExpression build() {
    return Concrete.AppExpression.make(myData, myFunction, myArguments);
  }

  @Override
  public @NotNull ConcreteAppBuilder app(@NotNull ConcreteExpression argument, boolean isExplicit) {
    if (!(argument instanceof Concrete.Expression)) {
      throw new IllegalArgumentException();
    }
    myArguments.add(new Concrete.Argument((Concrete.Expression) argument, isExplicit));
    return this;
  }

  @Override
  public @NotNull ConcreteAppBuilder app(@NotNull ConcreteExpression argument) {
    app(argument, true);
    return this;
  }

  @Override
  public @NotNull ConcreteAppBuilder app(@NotNull ConcreteArgument argument) {
    if (!(argument instanceof Concrete.Argument)) {
      throw new IllegalArgumentException();
    }
    myArguments.add((Concrete.Argument) argument);
    return this;
  }

  @Override
  public @NotNull ConcreteAppBuilder app(@NotNull Collection<? extends ConcreteArgument> arguments) {
    for (ConcreteArgument argument : arguments) {
      app(argument);
    }
    return this;
  }
}
