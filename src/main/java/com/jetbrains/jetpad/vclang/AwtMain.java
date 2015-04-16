package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.model.Example;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.toView.CellToView;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.projectional.view.*;
import jetbrains.jetpad.projectional.view.toAwt.ViewContainerComponent;

import javax.swing.*;
import java.awt.*;

public class AwtMain {
  public static void main(String[] args) {
    final CellContainer container = ContainerFactory.getMainContainer();
    container.root.children().add(Example.create().getTarget());

    final ViewContainer viewContainer = new ViewContainer();
    CellToView.map(container, viewContainer);

    final CellContainer errorsContainer = ContainerFactory.getErrorsContainer();
    final ViewContainer errorsViewContainer = new ViewContainer();
    CellToView.map(errorsContainer, errorsViewContainer);

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

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        JFrame frame = new JFrame("ViewContainer");
        frame.setLayout(new BorderLayout());

        final ViewContainerComponent component = new ViewContainerComponent();
        component.container(viewContainer);
        ViewContainerComponent errorsComp = new ViewContainerComponent();
        errorsComp.container(errorsViewContainer);

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            component.requestFocus();
          }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, component, errorsComp);
        frame.add(splitPane, BorderLayout.CENTER);
        splitPane.setDividerLocation(900);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
      }
    });
  }

  private static String load() {
    return null; //TODO
  }
}
