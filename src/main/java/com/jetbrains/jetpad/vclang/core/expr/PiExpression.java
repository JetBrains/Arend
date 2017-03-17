package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class PiExpression extends DependentTypeExpression {
  private final Level myPLevel;
  private final Expression myCodomain;

  @Deprecated
  public PiExpression(SingleDependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
    myPLevel = new Level(0);
    myCodomain = codomain;
  }

  public PiExpression(Level pLevel, SingleDependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
    myPLevel = pLevel;
    myCodomain = codomain;
  }

  public Level getPLevel() {
    return myPLevel;
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

  @Override
  public SingleDependentLink getParameters() {
    return (SingleDependentLink) super.getParameters();
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
}
