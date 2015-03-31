package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.model.expr.Model.AppExpression;
import static jetbrains.jetpad.cell.util.CellFactory.*;

public class AppExpressionMapper extends ExpressionMapper<AppExpression, AppExpressionMapper.Cell> {
  public AppExpressionMapper(AppExpression source) {
    super(source, new AppExpressionMapper.Cell(source.prec() > Abstract.AppExpression.PREC));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forExpression(this, getSource().function(), getTarget().function, "<fun>"));
    conf.add(forExpression(this, getSource().argument(), getTarget().argument, "<arg>"));
  }

  public static class Cell extends IndentCell {
    public jetbrains.jetpad.cell.Cell function = noDelete(indent());
    public jetbrains.jetpad.cell.Cell argument = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(label("("));
      CellFactory.to(this,
          function,
          space(),
          argument);
      if (parens) children().add(label(")"));

      focusable().set(true);
    }
  }
}
