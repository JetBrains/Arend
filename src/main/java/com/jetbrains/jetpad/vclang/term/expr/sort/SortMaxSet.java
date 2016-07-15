package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

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

  public void add(Sort sort) {
    if (mySorts.isEmpty()) {
      mySorts = new ArrayList<>();
    }
    mySorts.add(sort);
  }

  public void addAll(SortMaxSet sorts) {
    if (mySorts.isEmpty()) {
      mySorts = new ArrayList<>();
    }
    mySorts.addAll(sorts.mySorts);
  }

  public Type toType() {
    if (mySorts.isEmpty()) {
      return new UniverseExpression(Sort.PROP);
    }
    if (mySorts.size() == 1) {
      return new UniverseExpression(mySorts.get(0));
    }
    return new PiUniverseType(EmptyDependentLink.getInstance(), this);
  }

  public boolean isLessOrEquals(SortMaxSet sorts) {
    loop:
    for (Sort sort : mySorts) {
      for (Sort sort1 : sorts.mySorts) {
        if (sort.isLessOrEquals(sort1)) {
          continue loop;
        }
      }
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    if (mySorts.isEmpty()) {
      return "\\Prop";
    }
    if (mySorts.size() == 1) {
      return mySorts.get(0).toString();
    }

    String result = "\\Type (max";
    for (Sort sort : mySorts) {
      result += " (" + sort.getPLevel() + "," + sort.getHLevel() + ")";
    }
    return result + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SortMaxSet that = (SortMaxSet) o;

    return mySorts.equals(that.mySorts);

  }

  @Override
  public int hashCode() {
    return mySorts.hashCode();
  }
}
