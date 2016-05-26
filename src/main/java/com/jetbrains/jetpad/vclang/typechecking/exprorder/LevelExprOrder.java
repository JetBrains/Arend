package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.LevelExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;

public class LevelExprOrder implements ExpressionOrder {
  public static Boolean compareLevel(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new LevelExprOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxLevel(Expression expr1, Expression expr2) {
    return new LevelExprOrder().max(expr1, expr2);
  }

  public static boolean isZero(Expression expr) {
    LevelExpression level = expr.toLevel();
    if (level == null) return false;
    return level.isZero();
  }

  @Override
  public boolean isComparable(Expression expr) {
    return expr.toLevel() != null;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    LevelExpression level1 = expectedCMP == Equations.CMP.GE ? expr1.toLevel() : expr2.toLevel();
    LevelExpression level2 = expectedCMP == Equations.CMP.GE ? expr2.toLevel() : expr1.toLevel();

    if (level1 == null || level2 == null) {
      return null;
    }

    if (level1.isInfinity()) {
      return (expectedCMP != Equations.CMP.EQ || level2.isInfinity());
    }

    if (level2.isInfinity()) return false;

    if (level1.isUnit() && level2.isUnit()) {
      int sucs1 = level1.getUnitSucs();
      int sucs2 = level2.getUnitSucs();
      if (level1.isClosed()) {
        return level2.isClosed() && (expectedCMP == Equations.CMP.EQ ? sucs1 == sucs2 : sucs1 >= sucs2);
      }
      if (sucs1 == 0) {
        if (sucs2 != 0) return null;
        if (level2.isClosed()) return true;
        if (level1.getUnitBinding().equals(level2.getUnitBinding())) {
          return true;
        }
        return null;
      }
      return expectedCMP == Equations.CMP.GE ? visitor.compare(level1.subtract(sucs1), level2.subtract(sucs1)) : visitor.compare(level2.subtract(sucs1), level1.subtract(sucs1));
    }

    List<LevelExpression> level1MaxArgs = level1.toListOfMaxArgs();
    List<LevelExpression> level2MaxArgs = level2.toListOfMaxArgs();

    for (LevelExpression maxArg1 : level1MaxArgs) {
      boolean feasibleArg = true;
      int numSucs = maxArg1.extractOuterSucs();
      for (LevelExpression maxArg2 : level2MaxArgs) {
        if (numSucs < maxArg2.extractOuterSucs()) {
          feasibleArg = false;
          break;
        }
        if (maxArg1.isClosed()) {
          if (maxArg2.isClosed()) {
            continue;
          }
          feasibleArg = false;
          break;
        }
        if (!(expectedCMP == Equations.CMP.GE ? visitor.compare(maxArg1, maxArg2) : visitor.compare(maxArg2, maxArg1))) {
          feasibleArg = false;
          break;
        }
      }
      if (feasibleArg) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    LevelExpression level1 = expr1.toLevel();
    LevelExpression level2 = expr2.toLevel();

    LevelExpression.CMP cmp = level1.compare(level2);

    if (cmp == LevelExpression.CMP.LESS) {
      return level2;
    } else if (cmp == LevelExpression.CMP.GREATER || cmp == LevelExpression.CMP.EQUAL) {
      return level1;
    }

    return level1.max(level2);
  }
}
