package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.LevelEquation;

import java.util.ArrayList;
import java.util.List;

public class SolveLevelEquationsError extends LocalTypeCheckingError {
  public final List<? extends LevelEquation<? extends LevelVariable>> equations;

  public SolveLevelEquationsError(List<? extends LevelEquation<? extends LevelVariable>> equations, Abstract.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }

  @Override
  public Doc getBodyDoc(SourceInfoProvider src) {
    List<LineDoc> docs = new ArrayList<>(equations.size());
    StringBuilder builder = new StringBuilder();
    PrettyPrintVisitor ppv = new PrettyPrintVisitor(builder, src, 0);

    for (LevelEquation<? extends Variable> equation : equations) {
      builder.setLength(0);
      if (equation.isInfinity()) {
        printEqExpr(builder, ppv, equation.getVariable(), null);
        builder.append(" = inf");
      } else {
        printEqExpr(builder, ppv, equation.getVariable1(), -equation.getConstant());
        builder.append(" <= ");
        printEqExpr(builder, ppv, equation.getVariable2(), equation.getConstant());
      }
      docs.add(DocFactory.text(builder.toString()));
    }

    return DocFactory.vList(docs);
  }

  private void printEqExpr(StringBuilder builder, PrettyPrintVisitor ppv, Variable var, Integer constant) {
    if (var != null) {
      if (var instanceof InferenceLevelVariable) {
        ppv.prettyPrintInferLevelVar((InferenceLevelVariable) var);
      } else {
        builder.append(var);
      }
      if (constant != null && constant > 0) {
        builder.append(" + ").append(constant);
      }
    } else {
      builder.append(constant > 0 ? constant : 0);
    }
  }
}
