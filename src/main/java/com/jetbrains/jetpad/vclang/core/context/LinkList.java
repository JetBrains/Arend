package com.jetbrains.jetpad.vclang.core.context;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;

public class LinkList {
  private DependentLink myFirst = EmptyDependentLink.getInstance();
  private DependentLink myLast = EmptyDependentLink.getInstance();

  public DependentLink getFirst() {
    return myFirst;
  }

  public DependentLink getLast() {
    return myLast;
  }

  public void append(DependentLink link) {
    if (!link.hasNext()) {
      return;
    }

    if (myLast.hasNext()) {
      myLast.setNext(link);
    } else {
      myFirst = link;
    }
    myLast = DependentLink.Helper.getLast(link);
  }

  public void prepend(DependentLink link) {
    if (!link.hasNext()) {
      return;
    }

    if (myFirst.hasNext()) {
      link.setNext(myFirst);
    } else {
      myLast = DependentLink.Helper.getLast(link);
    }
    myFirst = link;
  }

  public boolean isEmpty() {
    return !myFirst.hasNext();
  }
}
