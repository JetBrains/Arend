package com.jetbrains.jetpad.vclang.term.definition;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeNames;

public class DataDefinition extends Definition {
  private final List<Constructor> myConstructors;

  public DataDefinition(String name, Signature signature, Precedence precedence, Fixity fixity, List<Constructor> constructors) {
    super(name, signature, precedence, fixity);
    myConstructors = constructors;
  }

  public DataDefinition(String name, Signature signature, Fixity fixity, List<Constructor> constructors) {
    super(name, signature, fixity);
    myConstructors = constructors;
  }

  public DataDefinition(String name, Signature signature, List<Constructor> constructors) {
    super(name, signature);
    myConstructors = constructors;
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public Constructor getConstructor(int index) {
    return myConstructors.get(index);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    builder.append("\\function\n");
    if (getFixity() == Fixity.PREFIX) {
      builder.append(getName());
    } else {
      builder.append('(').append(getName()).append(')');
    }
    getSignature().prettyPrint(builder, names, (byte) 0);
    for (Constructor constructor : myConstructors) {
      builder.append("\n    | ");
      constructor.prettyPrint(builder, names, (byte) 0);
    }
    removeNames(names, getSignature().getArguments());
  }
}
