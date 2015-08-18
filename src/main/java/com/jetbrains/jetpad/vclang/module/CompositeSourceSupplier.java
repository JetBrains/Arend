package com.jetbrains.jetpad.vclang.module;

import java.util.ArrayList;
import java.util.List;

public class CompositeSourceSupplier implements SourceSupplier {
  private final List<SourceSupplier> mySourceSuppliers;

  public CompositeSourceSupplier(List<SourceSupplier> sourceSuppliers) {
    mySourceSuppliers = sourceSuppliers;
  }

  @Override
  public Source getSource(Module module) {
    List<Source> sources = new ArrayList<>(mySourceSuppliers.size());
    for (SourceSupplier supplier : mySourceSuppliers) {
      sources.add(supplier.getSource(module));
    }
    return new CompositeSource(sources);
  }
}
