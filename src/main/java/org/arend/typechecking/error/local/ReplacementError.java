package org.arend.typechecking.error.local;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.typechecking.patternmatching.SubstitutionData;
import org.jetbrains.annotations.Nullable;

public class ReplacementError extends TypecheckingError {
  public final SubstitutionData substitutionData;

  public ReplacementError(SubstitutionData substitutionData, @Nullable ConcreteSourceNode cause) {
    super("Cannot perform substitution", cause);
    this.substitutionData = substitutionData;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return substitutionData.toDoc(ppConfig);
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
