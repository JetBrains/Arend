package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;

public class FieldReferableImpl extends LocatedReferableImpl implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;

  public FieldReferableImpl(Precedence precedence, String name, boolean isExplicit, boolean isParameter, LocatedReferable parent) {
    super(precedence, name, parent, Kind.FIELD);
    myExplicit = isExplicit;
    myParameter = isParameter;
  }

  @Override
  public boolean isExplicitField() {
    return myExplicit;
  }

  @Override
  public boolean isParameterField() {
    return myParameter;
  }
}
