package com.jetbrains.jetpad.vclang.editor.expr.arg;

import com.jetbrains.jetpad.vclang.model.expr.Model;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;

public class ArgumentMapperFactory implements MapperFactory<Model.Argument, Cell> {
  private final static ArgumentMapperFactory INSTANCE = new ArgumentMapperFactory();

  private ArgumentMapperFactory() {}

  @Override
  public Mapper<? extends Model.Argument, ? extends Cell> createMapper(Model.Argument source) {
    if (source instanceof Model.NameArgument) {
      return new NameArgumentMapper((Model.NameArgument) source);
    }
    if (source instanceof Model.TelescopeArgument) {
      return new TelescopeArgumentMapper((Model.TelescopeArgument) source);
    }
    if (source instanceof Model.TypeArgument) {
      return new TypeArgumentMapper((Model.TypeArgument) source);
    }
    return null;
  }

  public static ArgumentMapperFactory getInstance() {
    return INSTANCE;
  }
}
