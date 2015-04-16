package com.jetbrains.jetpad.vclang;

import com.google.common.io.Files;
import com.jetbrains.jetpad.vclang.model.Loader;
import com.jetbrains.jetpad.vclang.model.Root;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.toView.CellToView;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.json.JsonArray;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.otmodel.entity.event.EntityAdapter;
import jetbrains.jetpad.otmodel.entity.event.EntityEvent;
import jetbrains.jetpad.otmodel.json.Node2JsonConverter;
import jetbrains.jetpad.otmodel.node.ot.persistence.NodeIdPersistenceContext;
import jetbrains.jetpad.otmodel.ot.persistence.id.BaseIdCompressor;
import jetbrains.jetpad.otmodel.ot.persistence.id.UpdatableIdCompressor;
import jetbrains.jetpad.projectional.view.*;
import jetbrains.jetpad.projectional.view.toAwt.ViewContainerComponent;

import javax.swing.*;
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class AwtMain {
  public static void main(String[] args) {
    final CellContainer container = ContainerFactory.getMainContainer();
    container.root.children().add(Loader.create(load(), HOOK).getTarget());

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

  @NotNull
  private static File file() {
    return new File(System.getProperty("user.home"), ".vclang");
  }

  private static String load() {
    File file = file();
    if (file.exists()) {
      try {
        return Files.toString(file, SERIALIZATION_CHARSET);
      } catch (Throwable t) {
        System.out.println("can't create workbook from file content");
        t.printStackTrace();
      }
    } else {
      System.out.println("workbook file doesn't exist");
      try {
        file.createNewFile();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
    return null;
  }

  private static final Charset SERIALIZATION_CHARSET = Charset.forName("UTF-8");

  private static final Loader.Hook HOOK = new Loader.Hook() {
    @Override
    public void run(final Root root) {
      root.getContext().getEntityContext().addEntityListener(new EntityAdapter() {
        @Override
        protected void onEntityFeatureChanged(EntityEvent event) {
          BaseIdCompressor idCompressor = new UpdatableIdCompressor();
          NodeIdPersistenceContext persistenceContext = new NodeIdPersistenceContext(idCompressor);
          Node2JsonConverter converter = new Node2JsonConverter(persistenceContext);
          JsonArray jsonArray = converter.toJson(root.input.get().getNode());
          String json = jsonArray.toString();
          try {
            Files.write(json, file(), SERIALIZATION_CHARSET);
          } catch (IOException e) {
            System.out.println("problems with workbook saving");
            e.printStackTrace();
          }
        }
      });
    }
  };
}
