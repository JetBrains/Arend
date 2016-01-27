package com.jetbrains.jetpad.vclang.term.context;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;

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
      myLast = link;
    } else {
      myFirst = link;
      myLast = link;
    }
  }

  public void prepend(DependentLink link) {
    if (!link.hasNext()) {
      return;
    }

    if (myFirst.hasNext()) {
      assert !link.getNext().hasNext();
      link.setNext(myFirst);
      myFirst = link;
    } else {
      myFirst = link;
      myLast = link;
    }
  }

  public boolean isEmpty() {
    return !myFirst.hasNext();
  }
}
