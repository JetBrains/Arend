package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeNames;

public class Constructor extends Definition {
  private final DataDefinition myDataType;

  public Constructor(String name, List<TypeArgument> arguments, Fixity fixity, DataDefinition dataType) {
    super(name, new Signature(arguments, null), fixity);
    myDataType = dataType;
  }

  public DataDefinition getDataType() {
    return myDataType;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    builder.append(getName());
    for (TypeArgument argument : getSignature().getArguments()) {
      builder.append(' ');
      argument.prettyPrint(builder, names, Abstract.VarExpression.PREC);
    }
    removeNames(names, getSignature().getArguments());
  }
}
