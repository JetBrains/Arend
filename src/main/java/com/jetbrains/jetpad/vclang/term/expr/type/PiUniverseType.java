package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
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
  public PiUniverseType subst(ExprSubstitution substitution) {
    return new PiUniverseType(DependentLink.Helper.subst(myParameters, substitution), mySorts);
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
    List<DependentLink> params = new ArrayList<>();
    Expression cod = expression.getPiParameters(params, false, false);
    UniverseExpression uniCod = cod.toUniverse();
    if (uniCod == null) {
      return false;
    }

    int i = 0;
    for (DependentLink link = myParameters; link.hasNext(); link = link.getNext(), i++) {
      if (i == params.size() || !CompareVisitor.compare(equations, Equations.CMP.EQ, params.get(i).getType(), link.getType(), sourceNode)) {
        return false;
      }
    }

    return mySorts.isLessOrEquals(uniCod.getSort(), equations, sourceNode);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitPiUniverseType(this).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }
}
