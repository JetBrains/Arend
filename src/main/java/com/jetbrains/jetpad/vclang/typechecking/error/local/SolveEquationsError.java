package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equation;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class SolveEquationsError extends TypecheckingError {
  public final List<? extends Equation> equations;

  public SolveEquationsError(List<? extends Equation> equations, Concrete.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    List<Doc> docs = new ArrayList<>(equations.size());
    for (Equation equation : equations) {
      docs.add(hang(termDoc(equation.type),
                hang(text(equation.cmp == Equations.CMP.LE ? " <= " : equation.cmp == Equations.CMP.EQ ? " == " : " >= "),
                  termDoc(equation.expr))));
    }
    return vList(docs);
  }
}
