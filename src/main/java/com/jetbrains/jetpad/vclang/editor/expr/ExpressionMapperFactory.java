package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;

public class ExpressionMapperFactory implements MapperFactory<Expression, Cell> {
  @Override
  public Mapper<? extends Expression, ? extends Cell> createMapper(Expression source) {
    if (source instanceof LamExpression) {
      return new LamExpressionMapper((LamExpression)source);
    }
    return null;
  }
}
