package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.NelimExpression;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.mapper.Mapper;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;

public class NelimExpressionMapper extends Mapper<NelimExpression, TextCell> {
  public NelimExpressionMapper(NelimExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().text().set("N-elim");
  }
}
