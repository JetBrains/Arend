package com.jetbrains.jetpad.vclang.editor;

import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;

public class ModuleCell extends IndentCell {
  public final IndentCell definitions = new IndentCell();

  public ModuleCell() {
    CellFactory.to(this, definitions);
    focusable().set(true);
  }
}
