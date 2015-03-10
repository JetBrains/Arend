package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.ParensExpression;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.indent;
import static jetbrains.jetpad.cell.util.CellFactory.text;

public class ParensExpressionMapper extends Mapper<ParensExpression, ParensExpressionMapper.Cell> {
  public ParensExpressionMapper(ParensExpression source) {
    super(source, new ParensExpressionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forExpression(this, getSource().expression, getTarget().expression, "<expr>", ExpressionCompletion.getGlobalInstance()));
  }

  public static class Cell extends IndentCell {
    public jetbrains.jetpad.cell.Cell expression = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          text("("),
          expression,
          text(")"));

      focusable().set(true);
    }
  }
}
