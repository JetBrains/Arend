package org.arend.typechecking.instance.pool;

import org.arend.term.concrete.Concrete;

import java.util.List;

public class RecursiveInstanceHoleExpression extends Concrete.HoleExpression {
  public final List<? extends RecursiveInstanceData> recursiveData;

  public RecursiveInstanceHoleExpression(Object data, List<? extends RecursiveInstanceData> recursiveData) {
    super(data);
    this.recursiveData = recursiveData;
  }
}
