package com.jetbrains.jetpad.vclang;

import com.google.gwt.core.client.EntryPoint;
import com.jetbrains.jetpad.vclang.model.Example;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.toDom.CellContainerToDomMapper;

import static com.google.gwt.query.client.GQuery.$;

public class GwtMain implements EntryPoint {
  @Override
  public void onModuleLoad() {
    final CellContainer container = ContainerFactory.getMainContainer();
    container.root.children().add(Example.create().getTarget());

    new CellContainerToDomMapper(container, $("#proofDemo").get(0)).attachRoot();
  }

  private static String load() {
    return null; //TODO
  }
}
