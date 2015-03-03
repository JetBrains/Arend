package com.jetbrains.jetpad.vclang.editor.expr;

import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.cell.util.CellFactory.*;

public class LamExpressionCell extends IndentCell {
  public final TextCell variable = new TextCell();
  public final Cell body = indent();

  public LamExpressionCell() {
    CellFactory.to(this,
        text("λ"),
        variable,
        placeHolder(variable, "<no name>"),
        space(),
        text("=>"),
        space(),
        body);

    focusable().set(true);
    variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
    set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
  }
}
