package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import com.jetbrains.jetpad.vclang.term.expr.ErrorExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.property.ReadableProperty;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.values.Color;

import static jetbrains.jetpad.mapper.Synchronizers.forProperty;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsOneWay;

public class ExpressionMapper<E extends Model.Expression, C extends Cell> extends Mapper<E, C> {
  public ExpressionMapper(E source, C target) {
    super(source, target);
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsOneWay(getSource().wellTypedExpr(), new WritableProperty<Expression>() {
      @Override
      public void set(com.jetbrains.jetpad.vclang.term.expr.Expression value) {
        Node parent = (Node) getParent().getSource();
        if (value == null && parent instanceof Model.Expression) {
          ((Model.Expression) parent).wellTypedExpr().set(null);
        }
        if (value == null) {
          getTarget().background().set(null);
        } else {
          if (value instanceof ErrorExpression) {
            if (((ErrorExpression) value).expression() == null) {
              getTarget().background().set(Color.LIGHT_PINK);
            } else {
              getTarget().borderColor().set(Color.RED);
            }
          } else {
            getTarget().background().set(Color.LIGHT_GREEN);
          }
        }
      }
    }));
    conf.add(forProperty((ReadableProperty<?>) getSource().getPosition().getRole(), new Runnable() {
      @Override
      public void run() {
        getSource().wellTypedExpr().set(null);
      }
    }));
  }
}
