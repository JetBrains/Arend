package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class ListEquations implements Equations {
  private static class Equation {
    Expression expr1;
    Expression expr2;
    CMP cmp;
    int lifted;
  }

  private final List<Equation> myEquations = new ArrayList<>();

  @Override
  public void lift(int on) {
    if (on == 0) {
      return;
    }
    assert on < 0;

    for (Equation equation : myEquations) {
      if (equation.lifted < 0) {
        equation.lifted += on;
      } else {
        Expression expr1 = equation.expr1.liftIndex(0, on);
        Expression expr2 = equation.expr2.liftIndex(0, on);
        if (expr1 == null || expr2 == null) {
          equation.lifted += on;
        } else {
          equation.expr1 = expr1;
          equation.expr2 = expr2;
        }
      }
    }
  }

  @Override
  public boolean add(Equations equations) {
    if (equations instanceof ListEquations) {
      myEquations.addAll(((ListEquations) equations).myEquations);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean add(Expression expr1, Expression expr2, CMP cmp) {
    Equation equation = new Equation();
    equation.expr1 = expr1;
    equation.expr2 = expr2;
    equation.cmp = cmp;
    myEquations.add(equation);
    return true;
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
  public Equations newInstance() {
    return new ListEquations();
  }
}
