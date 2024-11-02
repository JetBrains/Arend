package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectingOrderingListener implements OrderingListener {
  public interface Element {
    void feedTo(OrderingListener listener);
    default Concrete.ResolvableDefinition getAnyDefinition() {
      return getAllDefinitions().get(0);
    }
    List<? extends Concrete.ResolvableDefinition> getAllDefinitions();
  }

  private static class MyHeader implements Element {
    final Concrete.ResolvableDefinition definition;

    private MyHeader(Concrete.ResolvableDefinition definition) {
      this.definition = definition;
    }

    @Override
    public void feedTo(OrderingListener listener) {
      listener.headerFound(definition);
    }

    @Override
    public Concrete.ResolvableDefinition getAnyDefinition() {
      return definition;
    }

    @Override
    public List<? extends Concrete.ResolvableDefinition> getAllDefinitions() {
      return Collections.singletonList(definition);
    }
  }

  private static class MyUnit implements Element {
    final Concrete.ResolvableDefinition definition;
    final boolean withLoops;

    MyUnit(Concrete.ResolvableDefinition definition, boolean withLoops) {
      this.definition = definition;
      this.withLoops = withLoops;
    }

    @Override
    public void feedTo(OrderingListener listener) {
      listener.unitFound(definition, withLoops);
    }

    @Override
    public Concrete.ResolvableDefinition getAnyDefinition() {
      return definition;
    }

    @Override
    public List<? extends Concrete.ResolvableDefinition> getAllDefinitions() {
      return Collections.singletonList(definition);
    }
  }

  private static class MyClass implements Element {
    private final Concrete.ClassDefinition definition;

    private MyClass(Concrete.ClassDefinition definition) {
      this.definition = definition;
    }

    @Override
    public void feedTo(OrderingListener listener) {
      listener.classFinished(definition);
    }

    @Override
    public Concrete.ResolvableDefinition getAnyDefinition() {
      return definition;
    }

    @Override
    public List<? extends Concrete.ResolvableDefinition> getAllDefinitions() {
      return Collections.singletonList(definition);
    }
  }

  private static class MyDefinitions implements Element {
    enum Kind { CYCLE, INSTANCE_CYCLE, PRE_BODIES, BODIES, USE }

    final List<? extends Concrete.ResolvableDefinition> definitions;
    final Kind kind;

    MyDefinitions(List<? extends Concrete.ResolvableDefinition> definitions, Kind kind) {
      this.definitions = definitions;
      this.kind = kind;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void feedTo(OrderingListener listener) {
      if (kind == MyDefinitions.Kind.USE) {
        listener.useFound((List<Concrete.FunctionDefinition>) definitions);
      } else if (kind == Kind.PRE_BODIES) {
        listener.preBodiesFound((List<Concrete.ResolvableDefinition>) definitions);
      } else if (kind == Kind.BODIES) {
        listener.bodiesFound((List<Concrete.ResolvableDefinition>) definitions);
      } else if (kind == Kind.CYCLE) {
        listener.cycleFound((List<Concrete.ResolvableDefinition>) definitions, false);
      } else if (kind == Kind.INSTANCE_CYCLE) {
        listener.cycleFound((List<Concrete.ResolvableDefinition>) definitions, true);
      } else {
        throw new IllegalStateException();
      }
    }

    @Override
    public List<? extends Concrete.ResolvableDefinition> getAllDefinitions() {
      return definitions;
    }
  }

  private final List<Element> myElements = new ArrayList<>();

  public boolean isEmpty() {
    return myElements.isEmpty();
  }

  public List<Element> getElements() {
    return myElements;
  }

  public List<Concrete.ResolvableDefinition> getAllDefinitions() {
    List<Concrete.ResolvableDefinition> result = new ArrayList<>();
    for (Element element : myElements) {
      result.addAll(element.getAllDefinitions());
    }
    return result;
  }

  @Override
  public void unitFound(Concrete.ResolvableDefinition unit, boolean recursive) {
    myElements.add(new MyUnit(unit, recursive));
  }

  @Override
  public void cycleFound(List<Concrete.ResolvableDefinition> definitions, boolean isInstance) {
    myElements.add(new MyDefinitions(definitions, isInstance ? MyDefinitions.Kind.INSTANCE_CYCLE : MyDefinitions.Kind.CYCLE));
  }

  @Override
  public void preBodiesFound(List<Concrete.ResolvableDefinition> definitions) {
    myElements.add(new MyDefinitions(definitions, MyDefinitions.Kind.PRE_BODIES));
  }

  @Override
  public void headerFound(Concrete.ResolvableDefinition definition) {
    myElements.add(new MyHeader(definition));
  }

  @Override
  public void bodiesFound(List<Concrete.ResolvableDefinition> bodies) {
    myElements.add(new MyDefinitions(bodies, MyDefinitions.Kind.BODIES));
  }

  @Override
  public void useFound(List<Concrete.FunctionDefinition> definitions) {
    myElements.add(new MyDefinitions(definitions, MyDefinitions.Kind.USE));
  }

  @Override
  public void classFinished(Concrete.ClassDefinition definition) {
    myElements.add(new MyClass(definition));
  }

  public void feed(OrderingListener listener) {
    for (Element element : myElements) {
      element.feedTo(listener);
    }
  }
}
