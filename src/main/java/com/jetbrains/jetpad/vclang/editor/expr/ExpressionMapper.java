package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ErrorExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.values.Color;

import static jetbrains.jetpad.mapper.Synchronizers.forProperty;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsOneWay;

public class ExpressionMapper<E extends Expression, C extends Cell> extends Mapper<E, C> {
  public ExpressionMapper(E source, C target) {
    super(source, target);
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsOneWay(getSource().wellTypedExpr(), new WritableProperty<com.jetbrains.jetpad.vclang.term.expr.Expression>() {
      @Override
      public void set(com.jetbrains.jetpad.vclang.term.expr.Expression value) {
        Node parent = getSource().parent().get();
        boolean setToNull = value == null;
        if (parent instanceof Expression) {
          if (value == null) {
            ((Expression) parent).wellTypedExpr().set(null);
          } else {
            setToNull = ((Expression) parent).wellTypedExpr().get() != null;
          }
        }
        getTarget().background().set(setToNull ? null : value instanceof ErrorExpression ? Color.LIGHT_PINK : Color.LIGHT_GREEN);

        getTarget().visible().set(false);
        getTarget().visible().set(true);
      }
    }));
    conf.add(forProperty((Property<?>) getSource().getPosition().getRole(), new Runnable() {
      @Override
      public void run() {
        getSource().wellTypedExpr().set(null);
      }
    }));
  }
}
