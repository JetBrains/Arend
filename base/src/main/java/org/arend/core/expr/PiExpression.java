package org.arend.core.expr;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.type.Type;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.expr.visitor.StripVisitor;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.InPlaceLevelSubstVisitor;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.expr.*;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.extImpl.AbstractedExpressionImpl;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PiExpression extends Expression implements Type, CorePiExpression, CoreAbsExpression {
  private Sort myResultSort;
  private final SingleDependentLink myLink;
  private final Expression myCodomain;

  public PiExpression(Sort resultSort, SingleDependentLink link, Expression codomain) {
    assert link.hasNext();
    myResultSort = resultSort;
    myLink = link;
    myCodomain = codomain;
  }

  public void substSort(LevelSubstitution substitution) {
    myResultSort = myResultSort.subst(substitution);
  }

  public static Sort generateUpperBound(Sort domSort, Sort codSort, Equations equations, Concrete.SourceNode sourceNode) {
    if (domSort.getPLevel().getVar() == null || codSort.getPLevel().getVar() == null || domSort.getPLevel().getVar() == codSort.getPLevel().getVar()) {
      return new Sort(domSort.getPLevel().max(codSort.getPLevel()), codSort.getHLevel());
    }

    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, false, sourceNode);
    equations.addVariable(pl);
    Level pLevel = new Level(pl);
    equations.addEquation(domSort.getPLevel(), pLevel, CMP.LE, sourceNode);
    equations.addEquation(codSort.getPLevel(), pLevel, CMP.LE, sourceNode);
    return new Sort(pLevel, codSort.getHLevel());
  }

  public Sort getResultSort() {
    return myResultSort;
  }

  @NotNull
  @Override
  public SingleDependentLink getParameters() {
    return myLink;
  }

  @NotNull
  @Override
  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public @NotNull AbstractedExpression getAbstractedCodomain() {
    return AbstractedExpressionImpl.make(myLink, myCodomain);
  }

  @Override
  public Expression applyExpression(Expression expression) {
    SingleDependentLink link = myLink;
    ExprSubstitution subst = new ExprSubstitution(link, expression);
    link = link.getNext();
    Expression result = myCodomain;
    if (link.hasNext()) {
      result = new PiExpression(myResultSort, link, result);
    }
    return result.subst(subst);
  }

  @Override
  public Expression applyExpression(Expression expression, boolean normalizing) {
    return applyExpression(expression);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitPi(this, param1, param2);
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
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
  public @NotNull CoreExpression getPiParameters(@Nullable List<? super CoreParameter> parameters) {
    return getPiParameters(parameters, false);
  }

  @Override
  public Expression getPiParameters(List<? super SingleDependentLink> params, boolean implicitOnly) {
    Expression cod = this;
    while (cod instanceof PiExpression) {
      PiExpression piCod = (PiExpression) cod;
      if (implicitOnly) {
        if (piCod.getParameters().isExplicit()) {
          break;
        }
        for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            return null;
          }
          if (params != null) {
            params.add(link);
          }
        }
      } else {
        if (params != null) {
          for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
            params.add(link);
          }
        }
      }
      cod = piCod.getCodomain().normalize(NormalizationMode.WHNF);
    }
    return cod;
  }

  @Override
  public void subst(InPlaceLevelSubstVisitor substVisitor) {
    substVisitor.visitPi(this, null);
  }

  @Override
  public PiExpression strip(StripVisitor visitor) {
    return visitor.visitPi(this, null);
  }

  @NotNull
  @Override
  public PiExpression normalize(@NotNull NormalizationMode mode) {
    return NormalizeVisitor.INSTANCE.visitPi(this, mode);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }

  @NotNull
  @Override
  public SingleDependentLink getBinding() {
    return myLink;
  }

  @NotNull
  @Override
  public Expression getExpression() {
    return myCodomain;
  }
}
