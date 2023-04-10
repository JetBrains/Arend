package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.error.SourceInfo;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.implicitargs.equations.LevelEquation;

import java.util.*;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class SolveLevelEquationsError extends TypecheckingError {
  public final Collection<? extends LevelEquation<? extends LevelVariable>> equations;

  public SolveLevelEquationsError(Collection<? extends LevelEquation<? extends LevelVariable>> equations, Concrete.SourceNode cause) {
    super("Cannot solve equations", cause);
    this.equations = equations;
  }

  public String getInferLevelVarText(PrettyPrintVisitor ppv, InferenceLevelVariable variable, Map<InferenceLevelVariable, Referable> refMap) {
    return ppv.getLevelVariableText(refMap.computeIfAbsent(variable, v -> new LocalReferable("?" + v.getName())), variable.getType());
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    List<Doc> docs = new ArrayList<>(equations.size());
    StringBuilder builder = new StringBuilder();
    PrettyPrintVisitor ppv = new PrettyPrintVisitor(builder, 0, !src.isSingleLine());
    Map<InferenceLevelVariable, Referable> refMap = new HashMap<>();

    Set<InferenceLevelVariable> variables = new HashSet<>();
    for (LevelEquation<? extends LevelVariable> equation : equations) {
      builder.setLength(0);
      if (equation.isInfinity()) {
        if (equation.getVariable() instanceof InferenceLevelVariable) {
          variables.add((InferenceLevelVariable) equation.getVariable());
        }

        printEqExpr(builder, ppv, equation.getVariable(), null, null, refMap);
        builder.append(" = \\oo");
      } else {
        if (equation.getVariable1() instanceof InferenceLevelVariable) {
          variables.add((InferenceLevelVariable) equation.getVariable1());
        }
        if (equation.getVariable2() instanceof InferenceLevelVariable) {
          variables.add((InferenceLevelVariable) equation.getVariable2());
        }

        printEqExpr(builder, ppv, equation.getVariable1(), -equation.getConstant(), null, refMap);
        builder.append(" <= ");
        printEqExpr(builder, ppv, equation.getVariable2(), equation.getConstant(), equation.getMaxConstant(), refMap);
      }
      docs.add(text(builder.toString()));
    }

    if (variables.isEmpty()) {
      return vList(docs);
    }

    if (variables.size() == 1) {
      InferenceLevelVariable variable = variables.iterator().next();
      Concrete.SourceNode sourceNode = variable.getSourceNode();
      if (sourceNode.getData() != getCause()) {
        SourceInfo sourceInfo = SourceInfo.getSourceInfo(sourceNode.getData());
        String position = sourceInfo != null ? sourceInfo.positionTextRepresentation() : null;
        docs.add(hang(hList(text("where " + getInferLevelVarText(ppv, variable, refMap) + " is defined"), position != null ? text(" at " + position) : empty(), text(" for")), ppDoc(sourceNode, src)));
      }
    } else {
      List<Doc> varDocs = new ArrayList<>(variables.size());
      for (InferenceLevelVariable variable : variables) {
        Concrete.SourceNode sourceNode = variable.getSourceNode();
        SourceInfo sourceInfo = SourceInfo.getSourceInfo(sourceNode.getData());
        String position = sourceInfo != null ? sourceInfo.positionTextRepresentation() : null;
        varDocs.add(hang(hList(text(getInferLevelVarText(ppv, variable, refMap)), position != null ? text(" at " + position) : empty(), text(" for")), ppDoc(sourceNode, src)));
      }
      docs.add(hang(text("where variables are defined as follows:"), vList(varDocs)));
    }
    return vList(docs);
  }

  private void printEqExpr(StringBuilder builder, PrettyPrintVisitor ppv, LevelVariable var, Integer constant, Integer maxConstant, Map<InferenceLevelVariable, Referable> refMap) {
    if (var != null) {
      boolean withMax = maxConstant != null && !(maxConstant < 0 || maxConstant == 0 && var.getType() == LevelVariable.LvlType.PLVL);
      if (withMax) {
        builder.append("max ");
        if (constant != null && constant > 0) {
          builder.append('(');
        }
      }
      builder.append(var instanceof InferenceLevelVariable ? getInferLevelVarText(ppv, (InferenceLevelVariable) var, refMap) : var);
      if (constant != null && constant > 0) {
        builder.append(" + ").append(constant);
        if (withMax) {
          builder.append(')');
        }
      }
      if (withMax) {
        builder.append(' ').append(maxConstant);
      }
    } else {
      builder.append(constant > 0 ? constant : 0);
    }
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
