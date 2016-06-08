package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

import java.util.ArrayList;
import java.util.List;

public class UnsolvedEquations extends TypeCheckingError {
  private final List<ListEquations.CmpEquation> myEquations;
  private final List<ListEquations.LevelCmpEquation> myLevelEquations;

  public UnsolvedEquations(List<ListEquations.CmpEquation> equations, List<ListEquations.LevelCmpEquation> lev_equations) {
    super("Internal error: some equations were not solved", lev_equations.isEmpty() ? equations.get(0).sourceNode : lev_equations.get(0).sourceNode);
    myEquations = equations;
    myLevelEquations = lev_equations;
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
    for (ListEquations.LevelCmpEquation equation : myLevelEquations) {
      builder.append('\n');
      PrettyPrintVisitor.printIndent(builder, PrettyPrintVisitor.INDENT / 2);
      equation.expr1.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, PrettyPrintVisitor.INDENT / 2);
      builder.append(" = ");
      equation.expr2.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, PrettyPrintVisitor.INDENT / 2);
    }
    return builder.toString();
  }
}
