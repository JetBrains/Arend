package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;

public class FieldReferableImpl extends LocatedReferableImpl implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;
  private final boolean myRealParameterField;

  public FieldReferableImpl(Precedence precedence, String name, boolean isExplicit, boolean isParameter, boolean isRealParameterField, LocatedReferable parent) {
    super(precedence, name, parent, Kind.FIELD);
    myExplicit = isExplicit;
    myParameter = isParameter;
    myRealParameterField = isRealParameterField;
  }

  public FieldReferableImpl(Precedence precedence, String name, boolean isExplicit, boolean isParameter, LocatedReferable parent) {
    this(precedence, name, isExplicit, isParameter, false, parent);
  }

  @Override
  public boolean isExplicitField() {
    return myExplicit;
  }

  @Override
  public boolean isParameterField() {
    return myParameter;
  }

  @Override
  public boolean isRealParameterField() {
    return myRealParameterField;
  }
}
