package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class PiExpression extends Expression implements Type {
  private final Sort myResultSort;
  private final SingleDependentLink myLink;
  private final Expression myCodomain;

  public PiExpression(Sort resultSort, SingleDependentLink link, Expression codomain) {
    assert link.hasNext();
    myResultSort = resultSort;
    myLink = link;
    myCodomain = codomain;
  }

  public static Sort generateUpperBound(Sort domSort, Sort codSort, Equations equations, Abstract.SourceNode sourceNode) {
    if ((domSort.getPLevel().getVar() == null || codSort.getPLevel().getVar() == null || domSort.getPLevel().getVar() == codSort.getPLevel().getVar())) {
      return new Sort(domSort.getPLevel().max(codSort.getPLevel()), codSort.getHLevel());
    }

    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, sourceNode);
    equations.addVariable(pl);
    Level pLevel = new Level(pl);
    equations.add(domSort.getPLevel(), pLevel, Equations.CMP.LE, sourceNode);
    equations.add(codSort.getPLevel(), pLevel, Equations.CMP.LE, sourceNode);
    return new Sort(pLevel, codSort.getHLevel());
  }

  public Sort getResultSort() {
    return myResultSort;
  }

  public SingleDependentLink getParameters() {
    return myLink;
  }

  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public PiExpression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return myResultSort;
  }

  @Override
  public PiExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new SubstVisitor(exprSubstitution, levelSubstitution).visitPi(this, null);
  }

  @Override
  public PiExpression strip(LocalErrorReporter errorReporter) {
    return new StripVisitor(errorReporter).visitPi(this, null);
  }

  @Override
  public PiExpression normalize(NormalizeVisitor.Mode mode) {
    return NormalizeVisitor.INSTANCE.visitPi(this, mode);
  }

  @Override
  public boolean isWHNF() {
    return true;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
