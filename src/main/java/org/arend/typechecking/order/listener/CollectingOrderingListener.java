package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.SCC;

import java.util.ArrayList;
import java.util.List;

public class CollectingOrderingListener implements OrderingListener {
  private static class DefinitionUnit {
    final Concrete.Definition definition;
    final boolean isHeaderOnly;
    final boolean isRecursive;

    public DefinitionUnit(Concrete.Definition definition, boolean isHeaderOnly, boolean isRecursive) {
      this.definition = definition;
      this.isHeaderOnly = isHeaderOnly;
      this.isRecursive = isRecursive;
    }
  }

  private final List<Object> myList = new ArrayList<>();

  @Override
  public void definitionFound(Concrete.Definition definition, boolean isHeaderOnly, boolean isRecursive) {
    myList.add(new DefinitionUnit(definition, isHeaderOnly, isRecursive));
  }

  @Override
  public void sccFound(SCC scc) {
    myList.add(scc);
  }

  public void feed(OrderingListener listener) {
    for (Object o : myList) {
      if (o instanceof SCC) {
        listener.sccFound((SCC) o);
      } else {
        DefinitionUnit unit = (DefinitionUnit) o;
        listener.definitionFound(unit.definition, unit.isHeaderOnly, unit.isRecursive);
      }
    }
  }
}
