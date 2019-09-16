package org.arend.typechecking.error.local;

import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.implicitargs.equations.Equation;
import org.arend.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

import static org.arend.error.doc.DocFactory.*;

public class SolveEquationsError extends TypecheckingError {
  public final List<? extends Equation> equations;

  public SolveEquationsError(List<? extends Equation> equations, Concrete.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    List<Doc> docs = new ArrayList<>(equations.size());
    for (Equation equation : equations) {
      docs.add(hang(termDoc(equation.expr1, ppConfig),
                hang(text(equation.cmp == Equations.CMP.LE ? " <= " : equation.cmp == Equations.CMP.EQ ? " == " : " >= "),
                  termDoc(equation.expr2, ppConfig))));
    }
    return vList(docs);
  }
}
