package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.*;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;

public class ExpressionMapperFactory implements MapperFactory<Expression, Cell> {
  private final static ExpressionMapperFactory INSTANCE = new ExpressionMapperFactory();

  private ExpressionMapperFactory() {}

  @Override
  public Mapper<? extends Expression, ? extends Cell> createMapper(Expression source) {
    if (source instanceof LamExpression) {
      return new LamExpressionMapper((LamExpression)source);
    }
    if (source instanceof AppExpression) {
      return new AppExpressionMapper((AppExpression)source);
    }
    if (source instanceof VarExpression) {
      return new VarExpressionMapper((VarExpression)source);
    }
    return null;
  }

  public static ExpressionMapperFactory getInstance() {
    return INSTANCE;
  }
}
