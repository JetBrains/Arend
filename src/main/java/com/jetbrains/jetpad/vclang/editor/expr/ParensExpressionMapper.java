package com.jetbrains.jetpad.vclang.editor.expr;

import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.ParensExpression;
import static jetbrains.jetpad.cell.util.CellFactory.indent;
import static jetbrains.jetpad.cell.util.CellFactory.label;

public class ParensExpressionMapper extends ExpressionMapper<ParensExpression, ParensExpressionMapper.Cell> {
  public ParensExpressionMapper(ParensExpression source) {
    super(source, new ParensExpressionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forExpression(this, getSource().expression(), getTarget().expression, "<expr>"));
  }

  public static class Cell extends IndentCell {
    public jetbrains.jetpad.cell.Cell expression = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          label("("),
          expression,
          label(")"));

      focusable().set(true);
    }
  }
}
