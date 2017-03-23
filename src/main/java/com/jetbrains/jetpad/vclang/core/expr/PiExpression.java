package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
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
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.Set;

public class PiExpression extends Expression implements Type {
  private final Level myPLevel;
  private final SingleDependentLink myLink;
  private final Expression myCodomain;

  public PiExpression(Level pLevel, SingleDependentLink link, Expression codomain) {
    assert link.hasNext();
    myPLevel = pLevel;
    myLink = link;
    myCodomain = codomain;
  }

  public static Level generateUpperBound(Level domPLevel, Level codPLevel, Equations equations, Abstract.SourceNode sourceNode) {
    if (domPLevel.getVar() == null || codPLevel.getVar() == null || domPLevel.getVar() == codPLevel.getVar()) {
      return domPLevel.max(codPLevel);
    }

    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, sourceNode);
    equations.addVariable(pl);
    Level pLevel = new Level(pl);
    equations.add(domPLevel, pLevel, Equations.CMP.LE, sourceNode);
    equations.add(codPLevel, pLevel, Equations.CMP.LE, sourceNode);
    return pLevel;
  }

  public Level getPLevel() {
    return myPLevel;
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
  public PiExpression toPi() {
    return this;
  }

  @Override
  public PiExpression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return new Sort(myPLevel, myCodomain.getType().toSort().getHLevel());
  }

  @Override
  public PiExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new SubstVisitor(exprSubstitution, levelSubstitution).visitPi(this, null);
  }

  @Override
  public PiExpression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return new StripVisitor(bounds, errorReporter).visitPi(this, null);
  }

  @Override
  public PiExpression normalize(NormalizeVisitor.Mode mode) {
    return new NormalizeVisitor(new EvalNormalizer()).visitPi(this, mode);
  }
}
