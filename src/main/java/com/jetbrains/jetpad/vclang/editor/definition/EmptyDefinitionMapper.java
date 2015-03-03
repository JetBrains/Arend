package com.jetbrains.jetpad.vclang.editor.definition;

import com.jetbrains.jetpad.vclang.model.definition.EmptyDefinition;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.mapper.Mapper;

public class EmptyDefinitionMapper extends Mapper<EmptyDefinition, TextCell> {
  EmptyDefinitionMapper(EmptyDefinition source) {
    super(source, new TextCell());
    getTarget().addTrait(TextEditing.validTextEditing(Validators.equalsTo("")));
  }
}
