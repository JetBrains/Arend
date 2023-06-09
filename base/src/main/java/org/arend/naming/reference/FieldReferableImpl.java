package org.arend.naming.reference;

import org.arend.ext.reference.Precedence;
import org.arend.term.group.AccessModifier;

public class FieldReferableImpl extends LocatedReferableImpl implements TCFieldReferable {
  private final boolean myExplicit;
  private final boolean myParameter;
  private final boolean myRealParameterField;

  public FieldReferableImpl(AccessModifier accessModifier, Precedence precedence, String name, boolean isExplicit, boolean isParameter, boolean isRealParameterField, LocatedReferable parent) {
    super(accessModifier, precedence, name, parent, Kind.FIELD);
    myExplicit = isExplicit;
    myParameter = isParameter;
    myRealParameterField = isRealParameterField;
  }

  public FieldReferableImpl(AccessModifier accessModifier, Precedence precedence, String name, boolean isExplicit, boolean isParameter, LocatedReferable parent) {
    this(accessModifier, precedence, name, isExplicit, isParameter, false, parent);
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
