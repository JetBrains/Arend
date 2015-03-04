package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.mapper.Mapper;

import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class VarExpressionMapper extends Mapper<VarExpression, TextCell> {
  public VarExpressionMapper(VarExpression source) {
    super(source, new TextCell());
    getTarget().focusable().set(true);
    getTarget().addTrait(TextEditing.validTextEditing(Validators.identifier()));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().name, getTarget().text()));
  }
}
