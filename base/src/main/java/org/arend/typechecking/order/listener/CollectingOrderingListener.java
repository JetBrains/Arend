package org.arend.typechecking.order.listener;

import org.arend.naming.reference.MetaReferable;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectingOrderingListener implements OrderingListener {
  private static class MyUnit {
    final Concrete.Definition definition;
    final boolean withLoops;

    MyUnit(Concrete.Definition definition, boolean withLoops) {
      this.definition = definition;
      this.withLoops = withLoops;
    }
  }

  private static class MyDefinitions {
    enum Kind { CYCLE, BODIES, USE }

    final List<? extends Concrete.Definition> definitions;
    final Kind kind;

    MyDefinitions(List<? extends Concrete.Definition> definitions, Kind kind) {
      this.definitions = definitions;
      this.kind = kind;
    }
  }

  private final List<Object> myList = new ArrayList<>();

  @Override
  public void unitFound(Concrete.Definition unit, boolean recursive) {
    myList.add(new MyUnit(unit, recursive));
  }

  @Override
  public void cycleFound(List<Concrete.Definition> definitions) {
    myList.add(new MyDefinitions(definitions, MyDefinitions.Kind.CYCLE));
  }

  @Override
  public void headerFound(Concrete.Definition definition) {
    myList.add(definition);
  }

  @Override
  public void bodiesFound(List<Concrete.Definition> bodies) {
    myList.add(new MyDefinitions(bodies, MyDefinitions.Kind.BODIES));
  }

  @Override
  public void useFound(List<Concrete.UseDefinition> definitions) {
    myList.add(new MyDefinitions(definitions, MyDefinitions.Kind.USE));
  }

  @Override
  public void metaFound(Collection<MetaReferable> metas) {
  }

  @SuppressWarnings("unchecked")
  public void feed(OrderingListener listener) {
    for (Object o : myList) {
      if (o instanceof MyUnit) {
        MyUnit unit = (MyUnit) o;
        listener.unitFound(unit.definition, unit.withLoops);
      } else if (o instanceof MyDefinitions) {
        MyDefinitions definitions = (MyDefinitions) o;
        if (definitions.kind == MyDefinitions.Kind.USE) {
          listener.useFound((List<Concrete.UseDefinition>) definitions.definitions);
        } else if (definitions.kind == MyDefinitions.Kind.BODIES) {
          listener.bodiesFound((List<Concrete.Definition>) definitions.definitions);
        } else {
          listener.cycleFound((List<Concrete.Definition>) definitions.definitions);
        }
      } else {
        listener.headerFound((Concrete.Definition) o);
      }
    }
  }
}
