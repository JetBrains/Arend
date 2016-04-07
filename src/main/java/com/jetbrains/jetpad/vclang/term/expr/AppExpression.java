package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class AppExpression extends Expression {
  private Expression myFunction;
  private List<Expression> myArguments;
  private List<EnumSet<Flag>> myFlags;

  public enum Flag { EXPLICIT, VISIBLE }
  public static final EnumSet<Flag> DEFAULT = EnumSet.of(Flag.EXPLICIT, Flag.VISIBLE);

  private void initialize(Expression function, Collection<? extends Expression> arguments, Collection<? extends EnumSet<Flag>> flags) {
    assert flags.size() <= arguments.size();
    assert !arguments.isEmpty();

    myFunction = function.getFunction();
    AppExpression app = function.toApp();
    if (app != null) {
      myArguments = new ArrayList<>(app.getArguments().size() + arguments.size());
      myArguments.addAll(app.getArguments());
      myArguments.addAll(arguments);

      myFlags = new ArrayList<>(app.myFlags.size() + arguments.size());
      myFlags.addAll(app.myFlags);
    } else {
      myFlags = new ArrayList<>(arguments.size());
    }

    myFlags.addAll(flags);
    if (flags.size() < arguments.size()) {
      for (int i = arguments.size() - flags.size(); i > 0; i--) {
        myFlags.add(DEFAULT);
      }
    }
  }

  public AppExpression(Expression function, Collection<? extends Expression> arguments, Collection<? extends EnumSet<Flag>> flags) {
    initialize(function, arguments, flags);
    if (myArguments == null) {
      myArguments = new ArrayList<>(arguments);
    }
  }

  public AppExpression(Expression function, List<Expression> arguments, Collection<? extends EnumSet<Flag>> flags) {
    assert arguments.size() >= flags.size();
    initialize(function, arguments, flags);
    if (myArguments == null) {
      myArguments = arguments;
    }
  }

  @Override
  public Expression getFunction() {
    return myFunction;
  }

  @Override
  public List<? extends Expression> getArguments() {
    return myArguments;
  }

  public List<? extends EnumSet<Flag>> getFlags() {
    return myFlags;
  }

  @Override
  public AppExpression addArgument(Expression argument, EnumSet<Flag> flag) {
    myArguments.add(argument);
    myFlags.add(flag);
    return this;
  }

  @Override
  public AppExpression addArguments(Collection<? extends Expression> arguments, Collection<? extends EnumSet<AppExpression.Flag>> flags) {
    myArguments.addAll(arguments);
    myFlags.addAll(flags);
    return this;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public Expression getType() {
    Expression functionType = myFunction.getType();
    if (functionType != null) {
      return functionType.applyExpressions(myArguments);
    } else {
      return null;
    }
  }

  @Override
  public AppExpression toApp() {
    return this;
  }
}
