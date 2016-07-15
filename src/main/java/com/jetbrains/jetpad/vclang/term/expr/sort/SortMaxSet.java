package com.jetbrains.jetpad.vclang.term.expr.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SortMaxSet {
  private List<Sort> mySorts;

  public SortMaxSet() {
    mySorts = Collections.emptyList();
  }

  public SortMaxSet(Sort sort) {
    mySorts = new ArrayList<>(1);
    mySorts.add(sort);
  }

  public Collection<? extends Sort> getSorts() {
    return mySorts;
  }

  public void addAll(SortMaxSet sorts) {
    if (mySorts.isEmpty()) {
      mySorts = new ArrayList<>();
    }
    mySorts.addAll(sorts.mySorts);
  }
}
