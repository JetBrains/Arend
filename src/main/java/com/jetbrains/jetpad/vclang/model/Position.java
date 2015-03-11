package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public enum Position {
  FUN_CLAUSE(0),
  ARG(0),
  PARENS(0),
  DEF_RESULT_TYPE(0),
  ARR_DOM(Abstract.PiExpression.PREC + 1),
  ARR_COD(Abstract.PiExpression.PREC),
  APP_FUN(Abstract.AppExpression.PREC),
  APP_ARG(Abstract.AppExpression.PREC + 1),
  LAM(Abstract.LamExpression.PREC);

  private final int myPrec;

  Position(int prec) {
    myPrec = prec;
  }

  public int prec() {
    return myPrec;
  }
}
