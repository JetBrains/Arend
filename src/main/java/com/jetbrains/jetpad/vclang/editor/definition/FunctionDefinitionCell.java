package com.jetbrains.jetpad.vclang.editor.definition;

import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static jetbrains.jetpad.cell.util.CellFactory.*;

public class FunctionDefinitionCell extends IndentCell {
  final TextCell name = noDelete(new TextCell());
  final Cell type = noDelete(indent());
  final Cell term = noDelete(indent());

  private static <T extends Cell> T noDelete(T cell) {
    cell.addTrait(new CellTrait() {
      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        if (event.is(Key.DELETE) || event.is(Key.BACKSPACE)) {
          event.consume();
          return;
        }
        super.onKeyPressed(cell, event);
      }
    });
    return cell;
  }

  FunctionDefinitionCell() {
    to(this,
        noDelete(keyword("function")),
        newLine(),
        name,
        placeHolder(name, "<no name>"),
        space(),
        text(":"),
        space(),
        type,
        space(),
        text("=>"),
        space(),
        term);

    focusable().set(true);
    name.addTrait(TextEditing.validTextEditing(Validators.identifier()));
    set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(name));
  }
}
