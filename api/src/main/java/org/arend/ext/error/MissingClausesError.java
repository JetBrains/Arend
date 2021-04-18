package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class MissingClausesError extends TypecheckingError {
  public final @NotNull List<? extends List<? extends CorePattern>> missingClauses;
  public final @NotNull CoreParameter parameters;
  public final @NotNull List<? extends CoreBinding> eliminatedParameters;
  public final boolean generateIdpPatterns;
  public int maxListSize = 10;

  public MissingClausesError(@NotNull List<? extends List<? extends CorePattern>> missingClauses, @NotNull CoreParameter parameters, @NotNull List<? extends CoreBinding> eliminatedParameters, boolean generateIdpPatterns, ConcreteSourceNode cause) {
    super("Some clauses are missing", cause);
    this.missingClauses = missingClauses;
    this.parameters = parameters;
    this.eliminatedParameters = eliminatedParameters;
    this.generateIdpPatterns = generateIdpPatterns;
  }

  public boolean isElim() {
    return !eliminatedParameters.isEmpty();
  }

  public List<? extends List<? extends CorePattern>> getLimitedMissingClauses() {
    return missingClauses.size() > maxListSize ? missingClauses.subList(0, maxListSize) : missingClauses;
  }

  public void setMaxListSize(@Nullable Integer maxSize) {
    maxListSize = maxSize == null ? missingClauses.size() : maxSize;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    PrettyPrinterConfigImpl modPPConfig = new PrettyPrinterConfigImpl(ppConfig);
    modPPConfig.normalizationMode = null;
    modPPConfig.expressionFlags = modPPConfig.expressionFlags.clone();
    modPPConfig.expressionFlags.remove(PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS);

    List<LineDoc> docs = new ArrayList<>();
    for (List<? extends CorePattern> missingClause : getLimitedMissingClauses()) {
      docs.add(hSep(text(", "), missingClause.stream().map(pattern -> pattern.prettyPrint(modPPConfig)).collect(Collectors.toList())));
    }
    if (docs.size() < missingClauses.size()) {
      docs.add(text("..."));
    }
    return vList(docs);
  }

  @Override
  public boolean isShort() {
    return false;
  }
}
