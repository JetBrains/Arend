package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.Expression;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;

public class ExpressionMapperFactory implements MapperFactory<Expression, Cell> {
  @Override
  public Mapper<? extends Expression, ? extends Cell> createMapper(Expression expression) {
    /*
    if (source instanceof FunctionDefinition) {
      return new FunctionDefinitionMapper((FunctionDefinition)source);
    }
    if (source instanceof EmptyDefinition) {
      return new EmptyDefinitionMapper((EmptyDefinition)source);
    }
    */
    return null;
  }
}
