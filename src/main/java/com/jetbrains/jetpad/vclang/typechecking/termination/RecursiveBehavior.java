package com.jetbrains.jetpad.vclang.typechecking.termination;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by user on 12/16/16.
 */
public class RecursiveBehavior<T> {
  public List<BaseCallMatrix.R> myBehavior;
  public List<String> myLabels;
  public BaseCallMatrix<T> myInitialCallMatrix;

  RecursiveBehavior(BaseCallMatrix<T> callMatrix) {
    myBehavior = new LinkedList<>();
    for (int i = 0; i < callMatrix.getHeight(); i++) myBehavior.add(callMatrix.getValue(i, i));
    myLabels = new LinkedList<>(Arrays.asList(callMatrix.getRowLabels()));
    if (myLabels.size() != myBehavior.size())
      throw new IllegalArgumentException();
    myInitialCallMatrix = callMatrix;
  }

  RecursiveBehavior(RecursiveBehavior<T> rb, int i) {
    if (i < 0 || i >= rb.getLength()) throw new IllegalArgumentException();
    myBehavior = new LinkedList<>(rb.myBehavior);
    myLabels = new LinkedList<>(rb.myLabels);
    myBehavior.remove(i);
    myLabels.remove(i);
    myInitialCallMatrix = rb.myInitialCallMatrix;
  }

  public int getLength() {
    return myBehavior.size();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof RecursiveBehavior) {
      RecursiveBehavior rb2 = (RecursiveBehavior) o;
      return myBehavior.equals(rb2.myBehavior) && myLabels.equals(rb2.myLabels);
    }
    return false;
  }

  boolean leq(RecursiveBehavior<T> r) {
    if (getLength() != r.getLength()) throw new IllegalArgumentException();
    for (int i = 0; i < getLength(); i++)
      if (!CallMatrix.rleq(myBehavior.get(i), r.myBehavior.get(i)))
        return false;
    return !this.equals(r) || myInitialCallMatrix.getCompositeLength() <= r.myInitialCallMatrix.getCompositeLength();
  }

  @Override
  public String toString() {
    String valueLine = "";
    String result = "";
    for (int i = 0; i < getLength(); i++) {
      String label = myLabels.get(i);
      CallMatrix.R r = myBehavior.get(i);
      valueLine += String.format("%" + (label.length() + 1) + "s", CallMatrix.rToChar(r));
      result += " " + label;
    }
    return result + "\n" + valueLine;
  }
}
