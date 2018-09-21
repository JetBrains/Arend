package org.arend.typechecking.termination;

import org.arend.util.StringFormat;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class RecursiveBehavior<T> {
  public final List<BaseCallMatrix.R> behavior;
  public final List<String> labels;
  public final BaseCallMatrix<T> initialCallMatrix;

  RecursiveBehavior(BaseCallMatrix<T> callMatrix) {
    behavior = new LinkedList<>();
    for (int i = 0; i < callMatrix.getHeight(); i++) behavior.add(callMatrix.getValue(i, i));
    labels = new LinkedList<>(Arrays.asList(callMatrix.getRowLabels()));
    if (labels.size() != behavior.size())
      throw new IllegalArgumentException();
    initialCallMatrix = callMatrix;
  }

  RecursiveBehavior(RecursiveBehavior<T> rb, int i) {
    if (i < 0 || i >= rb.getLength()) throw new IllegalArgumentException();
    behavior = new LinkedList<>(rb.behavior);
    labels = new LinkedList<>(rb.labels);
    behavior.remove(i);
    labels.remove(i);
    initialCallMatrix = rb.initialCallMatrix;
  }

  public int getLength() {
    return behavior.size();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof RecursiveBehavior) {
      RecursiveBehavior rb2 = (RecursiveBehavior) o;
      return behavior.equals(rb2.behavior) && labels.equals(rb2.labels);
    }
    return false;
  }

  boolean leq(RecursiveBehavior<T> r) {
    if (getLength() != r.getLength()) throw new IllegalArgumentException();
    for (int i = 0; i < getLength(); i++)
      if (!CallMatrix.rleq(behavior.get(i), r.behavior.get(i)))
        return false;
    return !this.equals(r) || initialCallMatrix.getCompositeLength() <= r.initialCallMatrix.getCompositeLength();
  }

  @Override
  public String toString() {
    StringBuilder labels = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (int i = 0; i < getLength(); i++) {
      String label = this.labels.get(i);
      labels.append(' ').append(label);
      CallMatrix.R r = behavior.get(i);
      values.append(StringFormat.rightPad(label.length() + 1, CallMatrix.rToChar(r)));
    }
    return labels.toString() + '\n' + values.toString();
  }
}
