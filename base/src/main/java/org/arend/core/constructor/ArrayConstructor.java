package org.arend.core.constructor;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.DConstructor;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.BranchKey;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.prelude.Prelude;

import java.util.Objects;

public class ArrayConstructor implements BranchKey {
  private final DConstructor myConstructor;
  private final boolean myWithElementsType;
  private final boolean myWithLength;

  public ArrayConstructor(boolean isEmpty, boolean withElementsType, boolean withLength) {
    myConstructor = isEmpty ? Prelude.EMPTY_ARRAY : Prelude.ARRAY_CONS;
    myWithElementsType = withElementsType;
    myWithLength = withLength;
  }

  public ArrayConstructor(DConstructor constructor, boolean withElementsType, boolean withLength) {
    assert constructor == Prelude.EMPTY_ARRAY || constructor == Prelude.ARRAY_CONS;
    myConstructor = constructor;
    myWithElementsType = withElementsType;
    myWithLength = withLength;
  }

  public DConstructor getConstructor() {
    return myConstructor;
  }

  public boolean withElementsType() {
    return myWithElementsType;
  }

  public boolean withLength() {
    return myWithLength;
  }

  @Override
  public DependentLink getParameters(ConstructorExpressionPattern pattern) {
    return myConstructor.getArrayParameters(pattern.getLevels().toLevelPair(), pattern.getArrayLength(), pattern.getArrayThisBinding(), pattern.getArrayElementsType());
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
