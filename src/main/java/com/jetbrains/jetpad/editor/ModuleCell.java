package com.jetbrains.jetpad.editor;

import jetbrains.jetpad.cell.indent.IndentCell;

public class ModuleCell extends IndentCell {
    public final IndentCell definitions = new IndentCell(false);
}
