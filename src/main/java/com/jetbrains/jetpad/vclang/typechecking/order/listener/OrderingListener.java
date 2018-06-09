package com.jetbrains.jetpad.vclang.typechecking.order.listener;

import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;

public interface OrderingListener {
  enum Recursion { NO, IN_HEADER, IN_BODY }
  void unitFound(TypecheckingUnit unit, Recursion recursion);
  void sccFound(SCC scc);
}
