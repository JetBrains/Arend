package org.arend.typechecking.order;

import org.arend.term.concrete.Concrete;

import java.util.List;

public class SCC {
  private final List<Concrete.Definition> myDefinitions;

  public SCC(List<Concrete.Definition> definitions) {
    myDefinitions = definitions;
  }

  public List<? extends Concrete.Definition> getDefinitions() {
    return myDefinitions;
  }
}
