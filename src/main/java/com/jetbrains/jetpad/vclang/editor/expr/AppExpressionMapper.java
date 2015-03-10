package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.composite.Composites;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.indent;
import static jetbrains.jetpad.cell.util.CellFactory.space;

public class AppExpressionMapper extends Mapper<AppExpression, AppExpressionMapper.Cell> {
  public AppExpressionMapper(AppExpression source) {
    super(source, new AppExpressionMapper.Cell());
    jetbrains.jetpad.cell.Cell firstFocusable = Composites.<jetbrains.jetpad.cell.Cell>firstFocusable(getTarget());
    if (firstFocusable != null) {
      firstFocusable.focus();
    }
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forExpression(this, getSource().function, getTarget().function, "<fun>", ExpressionCompletion.getAppFunInstance()));
    conf.add(forExpression(this, getSource().argument, getTarget().argument, "<arg>", ExpressionCompletion.getAppArgInstance()));
  }

  public static class Cell extends IndentCell {
    public jetbrains.jetpad.cell.Cell function = noDelete(indent());
    public jetbrains.jetpad.cell.Cell argument = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          function,
          space(),
          argument);

      focusable().set(true);
    }
  }
}
