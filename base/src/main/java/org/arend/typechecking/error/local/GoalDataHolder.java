package org.arend.typechecking.error.local;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.expr.Expression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.naming.reference.GeneratedLocalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;
import org.arend.typechecking.TypecheckingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class GoalDataHolder extends TypecheckingError {
  public final TypecheckingContext typecheckingContext;
  public final Map<Binding, Expression> bindingTypes;
  public final Expression expectedType;

  private final ExprSubstitution substitution;

  public GoalDataHolder(@NotNull Level level,
                        @NotNull String message,
                        @Nullable ConcreteSourceNode cause,
                        @Nullable TypecheckingContext typecheckingContext,
                        @NotNull Map<Binding, Expression> bindingTypes,
                        @Nullable Expression expectedType) {
    super(level, message, cause);
    this.typecheckingContext = typecheckingContext;
    this.bindingTypes = normalizeValues(bindingTypes);
    this.substitution = calculateSubstitution(typecheckingContext);
    this.expectedType = expectedType == null ? null : expectedType.subst(substitution).normalize(NormalizationMode.RNF);
  }

  @NotNull
  private static ExprSubstitution calculateSubstitution(TypecheckingContext typecheckingContext) {
    ExprSubstitution substitution = new ExprSubstitution();
    if (typecheckingContext != null) {
      for (Iterator<Map.Entry<Referable, Binding>> iterator = typecheckingContext.localContext.entrySet().iterator(); iterator.hasNext(); ) {
        Map.Entry<Referable, Binding> entry = iterator.next();
        if (entry.getKey() instanceof GeneratedLocalReferable && entry.getValue() instanceof EvaluatingBinding) {
          substitution.add(entry.getValue(), ((EvaluatingBinding) entry.getValue()).getExpression());
          iterator.remove();
        }
      }
    }
    return substitution;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    Doc expectedDoc = getExpectedDoc(ppConfig);
    Doc contextDoc = getContextDoc(ppConfig);
    return vList(expectedDoc, contextDoc);
  }

  @NotNull
  protected Doc getExpectedDoc(PrettyPrinterConfig ppConfig) {
    return expectedType == null ? nullDoc() : hang(text("Expected type:"), expectedType.prettyPrint(ppConfig));
  }

  @NotNull
  protected Doc getContextDoc(PrettyPrinterConfig ppConfig) {
    Map<Referable, Binding> context = typecheckingContext == null ? Collections.emptyMap() : typecheckingContext.localContext;
    if (!context.isEmpty()) {
      List<Doc> contextDocs = new ArrayList<>(context.size());
      for (Map.Entry<Referable, Binding> entry : context.entrySet()) {
        if (!entry.getValue().isHidden() && (!(entry.getKey() instanceof LocalReferable) || !((LocalReferable) entry.getKey()).isHidden())) {
          Expression type = bindingTypes.get(entry.getValue());
          if (type == null) type = entry.getValue().getTypeExpr();
          if (type != null) type = type.subst(substitution);
          contextDocs.add(hang(hList(entry.getKey() == null ? text("_") : refDoc(entry.getKey()), text(" :")), type == null ? text("{?}") : termDoc(type, ppConfig)));
        }
      }
      return contextDocs.isEmpty() ? nullDoc() : hang(text("Context:"), vList(contextDocs));
    }
    return nullDoc();
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }

  private Map<Binding, Expression> normalizeValues(Map<Binding, Expression> baseMap) {
    Map<Binding, Expression> newMap = new LinkedHashMap<>(baseMap.size());
    for (Map.Entry<Binding, Expression> entry : baseMap.entrySet()) {
      newMap.put(entry.getKey(), entry.getValue().normalize(NormalizationMode.RNF));
    }
    return newMap;
  }
}
