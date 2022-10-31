package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;

import java.util.List;

public interface OrderingListener {
  void unitFound(Concrete.ResolvableDefinition definition, boolean recursive);
  void cycleFound(List<Concrete.ResolvableDefinition> definitions);
  void preBodiesFound(List<Concrete.Definition> definitions);
  void headerFound(Concrete.Definition definition);
  void bodiesFound(List<Concrete.Definition> definitions);
  void useFound(List<Concrete.UseDefinition> definitions);
  void classFinished(Concrete.ClassDefinition definition);
}
