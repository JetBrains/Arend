package org.arend.typechecking.termination;

class TestVertex {
  public final String myName;
  public final String[] myArguments;

  TestVertex(String name, String... arguments) {
    myName = name;
    myArguments = arguments;
  }

  @Override
  public String toString() {
    return myName;
  }
}
