package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Model;
import com.jetbrains.jetpad.vclang.term.expr.ErrorExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MappingContext;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.values.Color;

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
      public void set(Expression value) {
        Node parent = (Node) getParent().getSource();
        if (value == null && parent instanceof Model.Expression) {
          ((Model.Expression) parent).wellTypedExpr().set(null);
        }
        if (value == null) {
          getTarget().background().set(null);
          for (Node node : getSource().children()) {
            Mapper<?, ?> mapper = getDescendantMapper(node);
            if (mapper instanceof ExpressionMapper) {
              Expression cvalue = ((Model.Expression) mapper.getSource()).wellTypedExpr().get();
              if (cvalue != null) {
                ((ExpressionMapper) mapper).updateBackground(cvalue);
              }
            }
          }
        } else {
          updateBackground(value);
        }
      }
    }));
  }

  @Override
  protected void onAttach(MappingContext ctx) {
    super.onAttach(ctx);
    getSource().wellTypedExpr().set(null);
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    getSource().wellTypedExpr().set(null);
  }

  private void updateBackground(Expression value) {
    if (value instanceof ErrorExpression) {
      getTarget().background().set(Color.LIGHT_PINK);
    } else {
      getTarget().background().set(Color.LIGHT_GREEN);
    }
    for (Node node : getSource().children()) {
      Mapper<?, ?> mapper = getDescendantMapper(node);
      if (mapper != null) {
        ((Cell) mapper.getTarget()).background().set(null);
      }
    }
  }
}
