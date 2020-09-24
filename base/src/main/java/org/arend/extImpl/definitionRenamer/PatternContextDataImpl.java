package org.arend.extImpl.definitionRenamer;

import org.arend.core.expr.Expression;
import org.arend.ext.typechecking.PatternContextData;
import org.arend.extImpl.BaseContextDataImpl;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

public class PatternContextDataImpl extends BaseContextDataImpl implements PatternContextData {
  private final Concrete.Pattern myPattern;

  public PatternContextDataImpl(Expression expectedType, Concrete.Pattern pattern) {
    super(expectedType);
    myPattern = pattern;
  }

  @NotNull
  @Override
  public Concrete.Pattern getMarker() {
    return myPattern;
  }
}
