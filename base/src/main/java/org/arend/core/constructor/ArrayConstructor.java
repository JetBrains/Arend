package org.arend.core.constructor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.DConstructor;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.BranchKey;
import org.arend.prelude.Prelude;

import java.util.Objects;

public class ArrayConstructor implements BranchKey {
  private final DConstructor myConstructor;
  private final boolean myWithElementsType;

  public ArrayConstructor(boolean isEmpty, boolean withElementsType) {
    myConstructor = isEmpty ? Prelude.EMPTY_ARRAY : Prelude.ARRAY_CONS;
    myWithElementsType = withElementsType;
  }

  public ArrayConstructor(DConstructor constructor, boolean withElementsType) {
    assert constructor == Prelude.EMPTY_ARRAY || constructor == Prelude.ARRAY_CONS;
    myConstructor = constructor;
    myWithElementsType = withElementsType;
  }

  public DConstructor getConstructor() {
    return myConstructor;
  }

  public boolean withElementsType() {
    return myWithElementsType;
  }

  @Override
  public int getNumberOfParameters() {
    return DependentLink.Helper.size(getParameters());
  }

  @Override
  public DependentLink getParameters() {
    return myWithElementsType ? myConstructor.getParameters().getNext() : myConstructor.getParameters();
  }

  @Override
  public Body getBody() {
    return myConstructor.getBody();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArrayConstructor that = (ArrayConstructor) o;
    return myConstructor.equals(that.myConstructor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myConstructor);
  }
}
