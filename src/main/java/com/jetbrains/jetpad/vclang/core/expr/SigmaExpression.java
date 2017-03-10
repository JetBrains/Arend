package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class SigmaExpression extends DependentTypeExpression {
  private final Sort mySort;

  public SigmaExpression(Sort sort, DependentLink link) {
    super(link);
    assert link != null;
    mySort = sort;
  }

  public Sort getSort() {
    return mySort;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSigma(this, params);
  }

  @Override
  public SigmaExpression toSigma() {
    return this;
  }

  private static Sort getUniqueUpperBound(List<Sort> domSorts) {
    LevelVariable pVar = null;
    LevelVariable hVar = null;
    for (Sort sort : domSorts) {
      if (sort.getPLevel().getVar() != null) {
        if (pVar != sort.getPLevel().getVar()) {
          return null;
        }
        if (pVar == null) {
          pVar = sort.getPLevel().getVar();
        }
      }
      if (sort.getHLevel().getVar() != null) {
        if (hVar != sort.getHLevel().getVar()) {
          return null;
        }
        if (hVar == null) {
          hVar = sort.getHLevel().getVar();
        }
      }
    }

    if (domSorts.isEmpty()) {
      return Sort.PROP;
    } else {
      Sort resultSort = domSorts.get(0);
      for (int i = 1; i < domSorts.size(); i++) {
        resultSort = resultSort.max(domSorts.get(i));
      }
      return resultSort;
    }
  }

  public static Sort getUpperBound(List<Sort> sorts, Equations equations, Abstract.SourceNode sourceNode) {
    Sort resultSort = getUniqueUpperBound(sorts);
    if (resultSort != null) {
      return resultSort;
    }

    LevelArguments levelArguments = LevelArguments.generateInferVars(equations, sourceNode);
    for (Sort sort : sorts) {
      equations.add(sort.getPLevel(), levelArguments.getPLevel(), Equations.CMP.LE, sourceNode);
      equations.add(sort.getHLevel(), levelArguments.getHLevel(), Equations.CMP.LE, sourceNode);
    }
    return new Sort(levelArguments.getPLevel(), levelArguments.getHLevel());
  }
}
