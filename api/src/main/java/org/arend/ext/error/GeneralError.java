package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

public class GeneralError {
  public final @NotNull String message;
  public final @NotNull Level level;

  public enum Level { INFO, WARNING_UNUSED {
    @Override
    public String toString() {
      return "WARNING";
    }
  }, GOAL, WARNING, ERROR }

  public enum Stage { TYPECHECKER, RESOLVER, PARSER, OTHER }

  public GeneralError(@NotNull Level level, @NotNull String message) {
    this.level = level;
    this.message = message;
  }

  public ConcreteSourceNode getCauseSourceNode() {
    return null;
  }

  public Object getCause() {
    ConcreteSourceNode sourceNode = getCauseSourceNode();
    return sourceNode != null ? sourceNode.getData() : null;
  }

  public LineDoc getPositionDoc(PrettyPrinterConfig ppConfig) {
    Object cause = getCause();
    if (cause instanceof Collection) {
      Iterator it = ((Collection) cause).iterator();
      if (it.hasNext()) {
        cause = it.next();
      } else {
        return DocFactory.empty();
      }
    }
    Object data = cause instanceof SourceInfo ? cause : cause instanceof DataContainer ? ((DataContainer) cause).getData() : null;
    return data instanceof SourceInfo ? DocFactory.refDoc(new SourceInfoReference((SourceInfo) data)) : DocFactory.empty();
  }

  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.text(message);
  }

  public final LineDoc getHeaderDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.hSep(DocFactory.text(" "), DocFactory.text("[" + level + "]"), DocFactory.hEnd(DocFactory.text(":"), getPositionDoc(ppConfig)), getShortHeaderDoc(ppConfig));
  }

  public Doc getCauseDoc(PrettyPrinterConfig ppConfig) {
    Object cause = getCause();
    if (cause instanceof ArendRef) {
      return DocFactory.refDoc((ArendRef) cause);
    }

    ConcreteSourceNode sourceNode = getCauseSourceNode();
    return sourceNode != null ? sourceNode.prettyPrint(ppConfig) : null;
  }

  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.nullDoc();
  }

  public final Doc getDoc(PrettyPrinterConfig ppConfig) {
    Doc causeDoc = getCauseDoc(ppConfig);
    return DocFactory.vHang(getHeaderDoc(ppConfig), DocFactory.vList(getBodyDoc(ppConfig), causeDoc == null ? DocFactory.nullDoc() : DocFactory.hang(DocFactory.text("In:"), causeDoc)));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterConfig.DEFAULT));
  }

  public String getShortMessage() {
    return DocStringBuilder.build(getShortHeaderDoc(PrettyPrinterConfig.DEFAULT));
  }

  @NotNull
  public Stage getStage() {
    return Stage.OTHER;
  }

  public void forAffectedDefinitions(BiConsumer<ArendRef, GeneralError> consumer) {
    Object cause = getCause();
    if (cause instanceof ArendRef) {
      consumer.accept((ArendRef) cause, this);
    } else if (cause instanceof Collection) {
      for (Object elem : ((Collection) cause)) {
        if (elem instanceof ArendRef) {
          consumer.accept((ArendRef) elem, this);
        }
      }
    }
  }

  public boolean isSevere() {
    return getCause() == null;
  }

  public boolean isShort() {
    return !hasExpressions();
  }

  public boolean hasExpressions() {
    return false;
  }
}
