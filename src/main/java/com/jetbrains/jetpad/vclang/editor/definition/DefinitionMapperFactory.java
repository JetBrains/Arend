package com.jetbrains.jetpad.vclang.editor.definition;

import com.jetbrains.jetpad.vclang.model.definition.EmptyDefinition;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;

public class DefinitionMapperFactory implements MapperFactory<Definition, Cell> {
  private static DefinitionMapperFactory INSTANCE = new DefinitionMapperFactory();

  private DefinitionMapperFactory() {}

  @Override
  public Mapper<? extends Definition, ? extends Cell> createMapper(Definition source) {
    if (source instanceof FunctionDefinition) {
      return new FunctionDefinitionMapper((FunctionDefinition)source);
    }
    if (source instanceof EmptyDefinition) {
      return new EmptyDefinitionMapper((EmptyDefinition)source);
    }
    return null;
  }

  public static DefinitionMapperFactory getInstance() {
    return INSTANCE;
  }
}
