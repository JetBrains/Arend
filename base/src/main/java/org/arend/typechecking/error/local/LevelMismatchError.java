package org.arend.typechecking.error.local;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LevelMismatchError extends TypecheckingError {
  public final @NotNull TargetKind kind;
  public final Sort actualSort;

  public enum TargetKind {
    LEMMA("lemma"),
    PROPERTY("property"),
    SIGMA_FIELD("sigma field");

    private final String representation;

    TargetKind(String representation) {
      this.representation = representation;
    }
  }

  public LevelMismatchError(TargetKind kind, Sort sort, @Nullable ConcreteSourceNode cause) {
    super("The type of a " + kind.representation + " must be a proposition", cause);
    this.kind = kind;
    actualSort = sort != null ? sort : new Sort(new org.arend.core.sort.Level(LevelVariable.PVAR), org.arend.core.sort.Level.INFINITY);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.hList(DocFactory.text("Actual level: "), DocFactory.termLine(new UniverseExpression(actualSort), ppConfig));
  }
}
