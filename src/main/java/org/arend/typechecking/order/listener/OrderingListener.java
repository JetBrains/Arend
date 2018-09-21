package org.arend.typechecking.order.listener;

import org.arend.typechecking.order.SCC;
import org.arend.typechecking.typecheckable.TypecheckingUnit;

public interface OrderingListener {
  enum Recursion { NO, IN_HEADER, IN_BODY }
  void unitFound(TypecheckingUnit unit, Recursion recursion);
  void sccFound(SCC scc);
}
