package com.jetbrains.jetpad.vclang.editor.expr;

import jetbrains.jetpad.cell.TextCell;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.NelimExpression;

public class NelimExpressionMapper extends ExpressionMapper<NelimExpression, TextCell> {
  public NelimExpressionMapper(NelimExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("N-elim");
  }
}
