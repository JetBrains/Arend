package com.jetbrains.jetpad.vclang.editor.definition;

import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.cell.util.CellFactory.*;

public class FunctionDefinitionCell extends IndentCell {
  final TextCell name = new TextCell();
  final Cell type = indent();
  final Cell term = indent();

  FunctionDefinitionCell() {
    to(this,
        keyword("function"),
        newLine(),
        name,
        placeHolder(name, "<no name>"),
        space(),
        text(":"),
        space(),
        type,
        space(),
        text("="),
        space(),
        term);

    focusable().set(true);
    name.addTrait(TextEditing.validTextEditing(Validators.identifier()));

    set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(name));
  }
}
