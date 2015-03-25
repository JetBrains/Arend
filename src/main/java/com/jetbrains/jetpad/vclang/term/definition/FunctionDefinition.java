package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FunctionDefinition extends Definition {
  private final Expression term;

  public FunctionDefinition(String name, Signature signature, Expression term) {
    super(name, signature);
    this.term = term;
  }

  public Expression getTerm() {
    return term;
  }

  public void prettyPrint(PrintStream stream, List<String> names, int prec) {
    stream.print("function\n" + getName() + " : ");
    getSignature().getType().prettyPrint(stream, names, 0);
    stream.print("\n    = ");
    term.prettyPrint(stream, names, 0);
  }

  @Override
  public String toString() {
    return "function " + getName() + " : " + getSignature().toString() + " = " + term.toString();
  }

  @Override
  public FunctionDefinition checkTypes(Map<String, Definition> globalContext, List<TypeCheckingError> errors) {
    super.checkTypes(globalContext, errors);
    Expression type = getSignature().getType();
    CheckTypeVisitor.OKResult result = term.checkType(globalContext, new ArrayList<Definition>(), type, errors);
    return result == null ? null : new FunctionDefinition(getName(), new Signature(result.type), result.expression);
  }
}
