package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;

import java.util.List;

public interface OrderingListener {
  void unitFound(Concrete.Definition definition, boolean recursive);
  void cycleFound(List<Concrete.Definition> definitions);
  void headerFound(Concrete.Definition definitions);
  void bodiesFound(List<Concrete.Definition> definitions);
}
