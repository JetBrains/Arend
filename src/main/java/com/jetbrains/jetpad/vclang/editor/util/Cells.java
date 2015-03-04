package com.jetbrains.jetpad.vclang.editor.util;

import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;

public class Cells {
  public static <T extends Cell> T noDelete(T cell) {
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
}
