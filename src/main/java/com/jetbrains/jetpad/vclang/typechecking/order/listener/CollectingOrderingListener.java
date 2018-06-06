package com.jetbrains.jetpad.vclang.typechecking.order.listener;

import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class CollectingOrderingListener implements OrderingListener {
  private final List<Object> myList = new ArrayList<>();

  @Override
  public void unitFound(TypecheckingUnit unit, Recursion recursion) {
    myList.add(new Pair<>(unit, recursion));
  }

  @Override
  public void sccFound(SCC scc) {
    myList.add(scc);
  }

  public void feed(OrderingListener listener) {
    for (Object o : myList) {
      if (o instanceof SCC) {
        listener.sccFound((SCC) o);
      } else {
        //noinspection unchecked
        Pair<TypecheckingUnit, Recursion> pair = (Pair<TypecheckingUnit, Recursion>) o;
        listener.unitFound(pair.proj1, pair.proj2);
      }
    }
  }
}
