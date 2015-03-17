package com.jetbrains.jetpad.vclang;

import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.toView.CellToView;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.projectional.view.*;
import jetbrains.jetpad.projectional.view.toAwt.AwtViewDemo;

public class AwtMain {
  public static void main(String[] args) {
    final CellContainer container = ContainerFactory.getContainer();
    ViewContainer viewContainer = new ViewContainer();
    CellToView.map(container, viewContainer);

    Cell firstFocusable = Composites.<Cell>firstFocusable(container.root);
    if (firstFocusable != null) {
      firstFocusable.focus();
    }

    viewContainer.root().addTrait(new ViewTraitBuilder()
        .on(ViewEvents.KEY_PRESSED, new ViewEventHandler<KeyEvent>() {
          @Override
          public void handle(View view, KeyEvent e) {
            if (e.getKeyChar() == '\\') {
              CompletionEvent event = new CompletionEvent(false);
              container.complete(event);
              if (event.isConsumed()) {
                e.consume();
              }
            }
          }
        })
        .build());

    AwtViewDemo.show(viewContainer);
  }
}
