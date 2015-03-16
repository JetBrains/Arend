package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.editor.definition.ArgumentMapper;
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
    if (source instanceof ZeroExpression) {
      return new ZeroExpressionMapper((ZeroExpression)source);
    }
    if (source instanceof NatExpression) {
      return new NatExpressionMapper((NatExpression)source);
    }
    if (source instanceof NelimExpression) {
      return new NelimExpressionMapper((NelimExpression)source);
    }
    if (source instanceof SucExpression) {
      return new SucExpressionMapper((SucExpression)source);
    }
    if (source instanceof UniverseExpression) {
      return new UniverseExpressionMapper((UniverseExpression)source);
    }
    if (source instanceof PiExpression) {
      return new PiExpressionMapper((PiExpression)source);
    }
    if (source instanceof ParensExpression) {
      return new ParensExpressionMapper((ParensExpression)source);
    }
    if (source instanceof Argument) {
      return new ArgumentMapper((Argument) source);
    }
    return null;
  }

  public static ExpressionMapperFactory getInstance() {
    return INSTANCE;
  }
}
