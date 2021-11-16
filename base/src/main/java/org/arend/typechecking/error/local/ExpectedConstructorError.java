package org.arend.typechecking.error.local;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.GlobalReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class ExpectedConstructorError extends TypecheckingError {
  public final GlobalReferable referable;
  public final DefCallExpression defCall;
  public final DependentLink parameter;
  public final List<Expression> caseExpressions;
  public final DependentLink patternParameters;
  public final DependentLink clauseParameters;
  private final boolean myConstructorOfData;

  public ExpectedConstructorError(GlobalReferable referable,
                                  @Nullable DefCallExpression defCall,
                                  @Nullable DependentLink parameter,
                                  Concrete.SourceNode cause,
                                  @Nullable List<Expression> caseExpressions,
                                  DependentLink patternParameters,
                                  @Nullable DependentLink clauseParameters) {
    super("", cause);
    this.referable = referable;
    this.defCall = defCall;
    this.parameter = parameter;
    this.caseExpressions = caseExpressions;
    this.patternParameters = patternParameters;
    this.clauseParameters = clauseParameters;

    boolean constructorOfData = false;
    if (defCall instanceof DataCallExpression) {
      for (Constructor constructor : ((DataCallExpression) defCall).getDefinition().getConstructors()) {
        if (constructor.getReferable() == referable) {
          constructorOfData = true;
          break;
        }
      }
    } else {
      constructorOfData = defCall != null && defCall.getDefinition() == Prelude.DEP_ARRAY && (referable == Prelude.EMPTY_ARRAY.getRef() || referable == Prelude.ARRAY_CONS.getRef());
    }

    myConstructorOfData = constructorOfData;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("'"), refDoc(referable), text("' is not a constructor"), defCall == null ? empty() : hList(text(" of data type "), myConstructorOfData ? termLine(defCall, ppConfig) : refDoc(defCall.getDefinition().getReferable())));
  }

  @NotNull
  @Override
  public Stage getStage() {
    return defCall == null ? Stage.RESOLVER : Stage.TYPECHECKER;
  }

  @Override
  public boolean hasExpressions() {
    return myConstructorOfData;
  }
}
