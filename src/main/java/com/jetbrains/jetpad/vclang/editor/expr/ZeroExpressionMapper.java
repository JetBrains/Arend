package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.Model;
import jetbrains.jetpad.cell.TextCell;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.ZeroExpression;

public class ZeroExpressionMapper extends ExpressionMapper<ZeroExpression, TextCell> {
  public ZeroExpressionMapper(Model.ZeroExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("0");
  }
}
