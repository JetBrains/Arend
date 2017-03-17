package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiExpression extends DependentTypeExpression {
  private final List<Level> myPLevels;
  private final Expression myCodomain;

  @Deprecated
  public PiExpression(DependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
    myPLevels = Collections.singletonList(new Level(0));
    myCodomain = codomain;
  }

  public PiExpression(List<Level> pLevels, DependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
    myPLevels = pLevels;
    myCodomain = codomain;
  }

  public List<Level> getPLevels() {
    return myPLevels;
  }

  private static Level getUniqueUpperBound(List<Level> domPLevels, Level codPLevel) {
    LevelVariable pVar = null;
    for (Level pLevel : domPLevels) {
      if (pLevel.getVar() != null) {
        if (pVar != pLevel.getVar()) {
          return null;
        }
        if (pVar == null) {
          pVar = pLevel.getVar();
        }
      }
    }

    if (domPLevels.isEmpty()) {
      return new Level(0);
    } else {
      Level pLevel = codPLevel;
      for (Level domPLevel : domPLevels) {
        pLevel = pLevel.max(domPLevel);
      }
      return pLevel;
    }
  }

  public static List<Level> generateUpperBound(List<Level> domPLevels, Level codPLevel, Equations equations, Abstract.SourceNode sourceNode) {
    Level resultPLevel = getUniqueUpperBound(domPLevels, codPLevel);
    if (resultPLevel != null) {
      return Collections.singletonList(resultPLevel);
    }
    return generateUpperBounds(domPLevels, codPLevel, equations, sourceNode);
  }

  public static List<Level> generateUpperBounds(List<Level> domPLevels, Level codPLevel, Equations equations, Abstract.SourceNode sourceNode) {
    if (domPLevels.isEmpty()) {
      return Collections.emptyList();
    }

    List<Level> resultPLevels = new ArrayList<>(domPLevels.size());
    for (Level domPLevel : domPLevels) {
      InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, sourceNode);
      equations.addVariable(pl);
      Level pLevel = new Level(pl);
      resultPLevels.add(pLevel);
      equations.add(domPLevel, pLevel, Equations.CMP.LE, sourceNode);
      if (!resultPLevels.isEmpty()) {
        equations.add(pLevel, resultPLevels.get(resultPLevels.size() - 1), Equations.CMP.LE, sourceNode);
      }
    }

    equations.add(codPLevel, resultPLevels.get(resultPLevels.size() - 1), Equations.CMP.LE, sourceNode);
    return resultPLevels;
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
