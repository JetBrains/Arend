package org.arend.typechecking.order.listener;

import org.arend.naming.reference.MetaReferable;
import org.arend.term.concrete.Concrete;

import java.util.Collection;
import java.util.List;

public interface OrderingListener {
  void unitFound(Concrete.Definition definition, boolean recursive);
  void cycleFound(List<Concrete.Definition> definitions);
  void headerFound(Concrete.Definition definition);
  void bodiesFound(List<Concrete.Definition> definitions);
  void useFound(List<Concrete.UseDefinition> definitions);
  void metaFound(Collection<MetaReferable> metas);
}
