package org.arend.typechecking.order.listener;

import org.arend.term.concrete.Concrete;
import org.arend.typechecking.order.SCC;

public interface OrderingListener {
  void definitionFound(Concrete.Definition definition, boolean isHeaderOnly, boolean isRecursive);
  void sccFound(SCC scc);
}
