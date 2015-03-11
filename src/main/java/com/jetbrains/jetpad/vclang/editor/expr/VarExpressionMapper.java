package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.composite.Composites;

import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static com.jetbrains.jetpad.vclang.editor.util.Validators.identifier;
import static jetbrains.jetpad.cell.text.TextEditing.validTextEditing;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class VarExpressionMapper extends Mapper<VarExpression, TextCell> {
  public VarExpressionMapper(VarExpression source) {
    super(source, new TextCell());
    noDelete(getTarget());
    getTarget().focusable().set(true);
    getTarget().addTrait(validTextEditing(identifier()));
    getTarget().addTrait(new CellTrait() {
      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        if (event.is(Key.SPACE)) {
          if (((TextCell)cell).isEnd()) {
            AppExpression appExpr = new AppExpression();
            Mapper<?, ?> parentMapper = getParent();
            if (getSource().position == Position.APP_ARG) {
              AppExpression parentExpr = ((AppExpression) getSource().parent().get());
              parentMapper = parentMapper.getParent();
              parentExpr.replaceWith(appExpr);
              appExpr.setFunction(parentExpr);
            } else {
              getSource().replaceWith(appExpr);
              appExpr.setFunction(getSource());
            }
            AppExpressionMapper appExprMapper = (AppExpressionMapper) parentMapper.getDescendantMapper(appExpr);
            Cell firstFocusable = Composites.firstFocusable(appExprMapper.getTarget().argument);
            if (firstFocusable != null) {
              firstFocusable.focus();
            }
            event.consume();
            return;
          }
          if (((TextCell)cell).isHome()) {
            AppExpression appExpr = new AppExpression();
            Mapper<?, ?> parent = getParent();
            boolean inAppArg = getSource().position == Position.APP_ARG;
            if (inAppArg) {
              AppExpression parentExpr = ((AppExpression) getSource().parent().get());
              Expression function = parentExpr.getFunction();
              parentExpr.setFunction(appExpr);
              appExpr.setFunction(function);
            } else {
              getSource().replaceWith(appExpr);
              appExpr.setArgument(getSource());
            }
            AppExpressionMapper.Cell appExprCell = ((AppExpressionMapper) parent.getDescendantMapper(appExpr)).getTarget();
            Cell firstFocusable = Composites.firstFocusable(inAppArg ? appExprCell.argument : appExprCell.function);
            if (firstFocusable != null) {
              firstFocusable.focus();
            }
            event.consume();
            return;
          }
        }
        super.onKeyPressed(cell, event);
      }
    });
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name(), getTarget().text()));
  }
}
