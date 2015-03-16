package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.NatExpression;
import jetbrains.jetpad.cell.TextCell;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;

public class NatExpressionMapper extends ExpressionMapper<NatExpression, TextCell> {
  public NatExpressionMapper(NatExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("N");
  }
}
