package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.SucExpression;
import jetbrains.jetpad.cell.TextCell;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;

public class SucExpressionMapper extends ExpressionMapper<SucExpression, TextCell> {
  public SucExpressionMapper(SucExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("S");
  }
}
