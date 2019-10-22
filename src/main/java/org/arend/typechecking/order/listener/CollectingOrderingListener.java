package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;

public class CollectingOrderingListener implements OrderingListener {
  private static class MyUnit {
    final Concrete.Definition definition;
    final boolean withLoops;

    public MyUnit(Concrete.Definition definition, boolean withLoops) {
      this.definition = definition;
      this.withLoops = withLoops;
    }
  }

  private static class MyDefinitions {
    final List<Concrete.Definition> definitions;
    final boolean isCycle;

    private MyDefinitions(List<Concrete.Definition> definitions, boolean isCycle) {
      this.definitions = definitions;
      this.isCycle = isCycle;
    }
  }

  private final List<Object> myList = new ArrayList<>();

  @Override
  public void unitFound(Concrete.Definition unit, boolean recursive) {
    myList.add(new MyUnit(unit, recursive));
  }

  @Override
  public void cycleFound(List<Concrete.Definition> definitions) {
    myList.add(new MyDefinitions(definitions, true));
  }

  @Override
  public void headerFound(Concrete.Definition definition) {
    myList.add(definition);
  }

  @Override
  public void bodiesFound(List<Concrete.Definition> bodies) {
    myList.add(new MyDefinitions(bodies, false));
  }

  public void feed(OrderingListener listener) {
    for (Object o : myList) {
      if (o instanceof MyUnit) {
        MyUnit unit = (MyUnit) o;
        listener.unitFound(unit.definition, unit.withLoops);
      } else if (o instanceof MyDefinitions) {
        MyDefinitions definitions = (MyDefinitions) o;
        if (definitions.isCycle) {
          listener.cycleFound(definitions.definitions);
        } else {
          listener.bodiesFound(definitions.definitions);
        }
      } else {
        listener.headerFound((Concrete.Definition) o);
      }
    }
  }
}
