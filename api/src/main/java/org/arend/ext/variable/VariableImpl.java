package org.arend.ext.variable;

public class VariableImpl implements Variable {
  private final String myName;

  public VariableImpl(String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }
}
