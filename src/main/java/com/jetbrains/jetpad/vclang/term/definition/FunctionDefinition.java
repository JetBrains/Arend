package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeNames;

public final class FunctionDefinition extends Definition {
  private final Expression myTerm;
  private final Arrow myArrow;

  public FunctionDefinition(String name, Signature signature, Precedence precedence, Fixity fixity, Arrow arrow, Expression term) {
    super(name, signature, precedence, fixity);
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Signature signature, Fixity fixity, Arrow arrow, Expression term) {
    super(name, signature, fixity);
    myArrow = arrow;
    myTerm = term;
  }

  public FunctionDefinition(String name, Signature signature, Arrow arrow, Expression term) {
    super(name, signature);
    myArrow = arrow;
    myTerm = term;
  }

  public Arrow getArrow() {
    return myArrow;
  }

  public Expression getTerm() {
    return myTerm;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    builder.append("\\function\n");
    if (getFixity() == Fixity.PREFIX) {
      builder.append(getName());
    } else {
      builder.append('(').append(getName()).append(')');
    }
    builder.append(" ");
    getSignature().prettyPrint(builder, names, (byte) 0);
    builder.append(myArrow == Arrow.RIGHT ? " => " : " <= ");
    myTerm.prettyPrint(builder, names, (byte) 0);
    removeNames(names, getSignature().getArguments());
  }

  @Override
  public FunctionDefinition checkTypes(Map<String, Definition> globalContext, List<TypeCheckingError> errors) {
    super.checkTypes(globalContext, errors);
    Expression type = getSignature().getType();
    CheckTypeVisitor.OKResult result = myTerm.checkType(globalContext, new ArrayList<Binding>(), type, errors);
    return result == null ? null : new FunctionDefinition(getName(), new Signature(result.type), getPrecedence(), getFixity(), myArrow, result.expression);
  }
}
