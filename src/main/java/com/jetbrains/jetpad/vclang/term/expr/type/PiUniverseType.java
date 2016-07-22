package com.jetbrains.jetpad.vclang.term.expr.type;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
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

  @Override
  public DependentLink getParameters() {
    return myParameters;
  }

  @Override
  public PiUniverseType normalize(NormalizeVisitor.Mode mode) {
    if (myParameters.hasNext() && mode == NormalizeVisitor.Mode.NF) {
      DependentLink params = DependentLink.Helper.clone(myParameters);
      for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
        params.setType(params.getType().normalize(NormalizeVisitor.Mode.NF));
      }
      return new PiUniverseType(params, mySorts);
    } else {
      return this;
    }
  }

  @Override
  public PiUniverseType strip() {
    if (!myParameters.hasNext()) {
      return this;
    }

    DependentLink params = DependentLink.Helper.clone(myParameters);
    for (DependentLink link = params; link.hasNext(); link = link.getNext()) {
      params.setType(params.getType().strip());
    }
    return new PiUniverseType(params, mySorts);
  }

  @Override
  public Expression toExpression() {
    // TODO [sorts]: use mySorts.toSort()
    Sort sort;
    if (mySorts.getHLevel().isMinimum()) {
      sort = Sort.PROP;
    } else {
      List<Level> pLevels = mySorts.getPLevel().toListOfLevels();
      if (pLevels.size() > 1) {
        return null;
      }
      List<Level> hLevels = mySorts.getHLevel().toListOfLevels();
      if (hLevels.size() > 1) {
        return null;
      }
      sort = new Sort(pLevels.isEmpty() ? new Level(0) : pLevels.get(0), hLevels.isEmpty() ? new Level(0) : hLevels.get(0));
    }

    return new PiExpression(myParameters, new UniverseExpression(sort));
  }

  public SortMax getSorts() {
    return mySorts;
  }

  @Override
  public SortMax toSorts() {
    return myParameters.hasNext() ? null : mySorts;
  }

  @Override
  public Type getImplicitParameters(List<DependentLink> params) {
    DependentLink link = myParameters;
    for (; link.hasNext() && !link.isExplicit(); link = link.getNext()) {
      params.add(link);
    }
    return new PiUniverseType(link, mySorts);
  }

  @Override
  public Type fromPiParameters(List<DependentLink> params) {
    return new PiUniverseType(params.isEmpty() ? EmptyDependentLink.getInstance() : params.get(0), mySorts);
  }

  @Override
  public PiUniverseType addParameters(DependentLink params) {
    return params.hasNext() ? new PiUniverseType(ExpressionFactory.params(params, myParameters), mySorts) : this;
  }

  @Override
  public PiUniverseType subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return new PiUniverseType(DependentLink.Helper.subst(myParameters, exprSubst, levelSubst), mySorts);
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
