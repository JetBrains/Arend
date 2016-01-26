package com.jetbrains.jetpad.vclang.term.context;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;

public class LinkList {
  private DependentLink myFirst;
  private DependentLink myLast;

  public DependentLink getFirst() {
    return myFirst;
  }

  public DependentLink getLast() {
    return myLast;
  }

  public void append(DependentLink link) {
    if (myLast == null) {
      myFirst = link;
      myLast = link;
    } else {
      myLast.setNext(link);
      myLast = link;
    }
  }

  public void prepend(DependentLink link) {
    if (myFirst == null) {
      myFirst = link;
      myLast = link;
    } else {
      assert link.getNext() == null;
      link.setNext(myFirst);
      myFirst = link;
    }
  }
}
