package com.jetbrains.jetpad.vclang;

import com.google.gwt.core.client.EntryPoint;
import jetbrains.jetpad.cell.toDom.CellContainerToDomMapper;

import static com.google.gwt.query.client.GQuery.$;

public class GwtMain implements EntryPoint {
  @Override
  public void onModuleLoad() {
    new CellContainerToDomMapper(ContainerFactory.getContainer(), $("#proofDemo").get(0)).attachRoot();
  }
}
