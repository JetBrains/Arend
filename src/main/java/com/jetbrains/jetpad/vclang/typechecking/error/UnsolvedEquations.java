package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

import java.util.ArrayList;
import java.util.List;

public class UnsolvedEquations extends TypeCheckingError {
  private final List<ListEquations.CmpEquation> myEquations;

  public UnsolvedEquations(List<ListEquations.CmpEquation> equations) {
    super("Internal error: some equations were not solved", equations.get(0).sourceNode);
    myEquations = equations;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(printHeader()).append(getMessage());
    for (ListEquations.CmpEquation equation : myEquations) {
      builder.append('\n');
      PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
      equation.expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, PrettyPrintVisitor.INDENT / 2);
      builder.append(" = ");
      equation.expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, PrettyPrintVisitor.INDENT / 2);
    }
    return builder.toString();
  }
}
