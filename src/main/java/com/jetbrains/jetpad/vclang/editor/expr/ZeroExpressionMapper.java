package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.ZeroExpression;
import jetbrains.jetpad.cell.TextCell;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;

public class ZeroExpressionMapper extends ExpressionMapper<ZeroExpression, TextCell> {
  public ZeroExpressionMapper(ZeroExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("0");
  }
}
