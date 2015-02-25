package com.jetbrains.jetpad.vclang.editor;

import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;
import com.jetbrains.jetpad.vclang.model.definition.Definition;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;

public class DefinitionMapperFactory implements MapperFactory<Definition, Cell> {
    @Override
    public Mapper<? extends Definition, ? extends Cell> createMapper(Definition source) {
        if (source instanceof FunctionDefinition) {
            return new FunctionDefinitionMapper((FunctionDefinition)source);
        }
        return null;
    }
}
