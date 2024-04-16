package org.arend.typechecking.error.local;

import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equation;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class SolveEquationsError extends TypecheckingError {
  private final ExpressionPrettifier myPrettifier;
  public final List<? extends Equation> equations;

  public SolveEquationsError(ExpressionPrettifier prettifier, List<? extends Equation> equations, Concrete.SourceNode cause) {
    super("Cannot solve equations", cause);
    myPrettifier = prettifier;
    this.equations = equations;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    List<Doc> docs = new ArrayList<>(equations.size());
    for (Equation equation : equations) {
      docs.add(hang(termDoc(equation.expr1, myPrettifier, ppConfig),
                hang(text(equation.cmp == CMP.LE ? " <= " : equation.cmp == CMP.EQ ? " == " : " >= "),
                  termDoc(equation.expr2, myPrettifier, ppConfig))));
    }
    return vList(docs);
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
