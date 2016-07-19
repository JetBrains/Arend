package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class PiUniverseType implements Type {
  private final DependentLink myParameters;
  private final SortMax mySorts;

  public PiUniverseType(DependentLink parameters, SortMax sorts) {
    myParameters = parameters;
    mySorts = sorts;
  }

  public DependentLink getParameters() {
    return myParameters;
  }

  public SortMax getSorts() {
    return mySorts;
  }

  @Override
  public SortMax toSorts() {
    return myParameters.hasNext() ? null : mySorts;
  }

  @Override
  public Type subst(ExprSubstitution substitution) {
    // TODO [sorts]
    return this;
  }

  @Override
  public Type applyExpressions(List<? extends Expression> expressions) {
    DependentLink link = myParameters;
    for (Expression ignored : expressions) {
      if (link.hasNext()) {
        link = link.getNext();
      } else {
        return null;
      }
    }
    return new PiUniverseType(link, mySorts);
  }

  @Override
  public boolean isLessOrEquals(Sort sort) {
    return !myParameters.hasNext() && mySorts.isLessOrEquals(sort);
  }

  @Override
  public boolean isLessOrEquals(Expression expression, Equations equations, Abstract.SourceNode sourceNode) {
    // TODO [sorts]
    return false;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    // TODO [sorts]
  }
}
