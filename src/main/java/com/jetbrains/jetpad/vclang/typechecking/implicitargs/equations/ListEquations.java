package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class ListEquations implements Equations {
  private static class Equation {
    Expression expr1;
    Expression expr2;
    CMP cmp;
  }

  private final List<Equation> myEquations = new ArrayList<>();

  @Override
  public void add(Equations equations) {
    if (equations instanceof ListEquations) {
      myEquations.addAll(((ListEquations) equations).myEquations);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public void add(Expression expr1, Expression expr2, CMP cmp) {
    Equation equation = new Equation();
    equation.expr1 = expr1;
    equation.expr2 = expr2;
    equation.cmp = cmp;
    myEquations.add(equation);
  }

  @Override
  public void clear() {
    myEquations.clear();
  }

  @Override
  public boolean isEmpty() {
    return myEquations.isEmpty();
  }

  @Override
  public void abstractBinding(Binding binding) {

  }

  @Override
  public Equations newInstance() {
    return new ListEquations();
  }
}
