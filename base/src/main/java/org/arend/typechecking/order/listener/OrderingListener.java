package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;

import java.util.List;

public interface OrderingListener {
  void unitFound(Concrete.ResolvableDefinition definition, boolean recursive);
  void cycleFound(List<Concrete.ResolvableDefinition> definitions);
  void preBodiesFound(List<Concrete.ResolvableDefinition> definitions);
  void headerFound(Concrete.ResolvableDefinition definition);
  void bodiesFound(List<Concrete.ResolvableDefinition> definitions);
  void useFound(List<Concrete.UseDefinition> definitions);
  void classFinished(Concrete.ClassDefinition definition);
}
