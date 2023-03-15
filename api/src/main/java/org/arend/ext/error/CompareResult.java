package org.arend.ext.error;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.level.CoreLevels;

public interface CompareResult {
  CoreExpression getWholeExpr1();
  CoreExpression getWholeExpr2();
  CoreExpression getSubExpr1();
  CoreExpression getSubExpr2();
  CoreLevels getLevels1();
  CoreLevels getLevels2();
}
