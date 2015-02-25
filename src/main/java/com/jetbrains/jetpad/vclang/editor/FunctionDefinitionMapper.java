package com.jetbrains.jetpad.vclang.editor;

import jetbrains.jetpad.mapper.Mapper;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;

import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class FunctionDefinitionMapper extends Mapper<FunctionDefinition, FunctionDefinitionCell> {
    public FunctionDefinitionMapper(FunctionDefinition source) {
        super(source, new FunctionDefinitionCell());
    }

    @Override
    protected void registerSynchronizers(SynchronizersConfiguration conf) {
        super.registerSynchronizers(conf);

        conf.add(forPropsTwoWay(getSource().name, getTarget().name.text()));
    }
}
