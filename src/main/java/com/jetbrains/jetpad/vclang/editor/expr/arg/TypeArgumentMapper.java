package com.jetbrains.jetpad.vclang.editor.expr.arg;

import com.jetbrains.jetpad.vclang.model.expr.Model;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.mapper.Mapper;

public class TypeArgumentMapper extends Mapper<Model.TypeArgument, TypeArgumentMapper.Cell> {
  public TypeArgumentMapper(Model.TypeArgument source) {
    super(source, new Cell());
  }

  public static class Cell extends IndentCell {

  }
}
