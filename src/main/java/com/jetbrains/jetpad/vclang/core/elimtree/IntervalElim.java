package com.jetbrains.jetpad.vclang.core.elimtree;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.List;

public class IntervalElim implements Body {
  private final DependentLink myParameters;
  private final List<Pair<Expression, Expression>> myCases;
  private final ElimTree myOtherwise;

  public IntervalElim(DependentLink parameters, List<Pair<Expression, Expression>> cases, ElimTree otherwise) {
    myParameters = parameters;
    myCases = cases;
    myOtherwise = otherwise;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public List<Pair<Expression, Expression>> getCases() {
    return myCases;
  }

  public ElimTree getOtherwise() {
    return myOtherwise;
  }
}
