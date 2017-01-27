package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Function;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.BranchElimTreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AppExpression extends Expression {
  private Expression myFunction;
  private List<Expression> myArguments;

  private void initialize(Expression function, Collection<? extends Expression> arguments) {
    assert !arguments.isEmpty();
    assert function.toConCall() == null && function.toDataCall() == null && function.toClassCall() == null;

    myFunction = function.getFunction();
    AppExpression app = function.toApp();
    if (app != null) {
      myArguments = new ArrayList<>(app.getArguments().size() + arguments.size());
      myArguments.addAll(app.getArguments());
      myArguments.addAll(arguments);
    }
  }

  public AppExpression(Expression function, Collection<? extends Expression> arguments) {
    initialize(function, arguments);
    if (myArguments == null) {
      myArguments = new ArrayList<>(arguments);
    }
  }

  public AppExpression(Expression function, List<Expression> arguments) {
    initialize(function, arguments);
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

  @Override
  public AppExpression addArgument(Expression argument) {
    myArguments.add(argument);
    return this;
  }

  @Override
  public AppExpression addArguments(Collection<? extends Expression> arguments) {
    myArguments.addAll(arguments);
    return this;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public AppExpression toApp() {
    return this;
  }

  @Override
  public Variable getStuckVariable() {
    if (myFunction instanceof ReferenceExpression && ((ReferenceExpression) myFunction).getBinding() instanceof Function) {
      Function function = (Function) ((ReferenceExpression) myFunction).getBinding();
      Binding binding = ((BranchElimTreeNode) function.getElimTree()).getReference();
      int i = 0;
      for (DependentLink param = function.getParameters(); param.hasNext(); param = param.getNext()) {
        if (param == binding) {
          return myArguments.get(i).getStuckVariable();
        }
        if (++i >= myArguments.size()) {
          // TODO: eta expand function calls
          return null;
        }
      }
    }
    return myFunction.getStuckVariable();
  }
}
