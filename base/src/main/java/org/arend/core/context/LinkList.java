package org.arend.core.context;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.jetbrains.annotations.NotNull;

public class LinkList {
  private DependentLink myFirst = EmptyDependentLink.getInstance();
  private DependentLink myLast = EmptyDependentLink.getInstance();

  @NotNull
  public DependentLink getFirst() {
    return myFirst;
  }

  @NotNull
  public DependentLink getLast() {
    return myLast;
  }

  public void clear() {
    myFirst = EmptyDependentLink.getInstance();
    myLast = EmptyDependentLink.getInstance();
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
      DependentLink.Helper.getLast(link).setNext(myFirst);
    } else {
      myLast = DependentLink.Helper.getLast(link);
    }
    myFirst = link;
  }

  public boolean isEmpty() {
    return !myFirst.hasNext();
  }
}
