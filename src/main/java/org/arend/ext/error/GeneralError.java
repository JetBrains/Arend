package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.DataContainer;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class GeneralError {
  public final String message;
  public final @Nonnull Level level;

  public enum Level { INFO, WEAK_WARNING {
    @Override
    public String toString() {
      return "WARNING";
    }
  }, GOAL, WARNING, ERROR }

  public enum Stage { META, TYPECHECKER, RESOLVER, PARSER, OTHER }

  public GeneralError(@Nonnull Level level, String message) {
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
        return empty();
      }
    }
    Object data = cause instanceof SourceInfo ? cause : cause instanceof DataContainer ? ((DataContainer) cause).getData() : null;
    return data instanceof SourceInfo ? refDoc(new SourceInfoReference((SourceInfo) data)) : empty();
  }

  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return text(message);
  }

  public final LineDoc getHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hSep(text(" "), text("[" + level + "]"), hEnd(text(":"), getPositionDoc(ppConfig)), getShortHeaderDoc(ppConfig));
  }

  public Doc getCauseDoc(PrettyPrinterConfig ppConfig) {
    Object cause = getCause();
    if (cause instanceof ArendRef) {
      return refDoc((ArendRef) cause);
    }

    ConcreteSourceNode sourceNode = getCauseSourceNode();
    return sourceNode != null ? sourceNode.prettyPrint(ppConfig) : null;
  }

  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return nullDoc();
  }

  public Doc getDoc(PrettyPrinterConfig ppConfig) {
    Doc causeDoc = getCauseDoc(ppConfig);
    return vHang(getHeaderDoc(ppConfig), vList(getBodyDoc(ppConfig), causeDoc == null ? nullDoc() : hang(text("In:"), causeDoc)));
  }

  @Override
  public final String toString() {
    return DocStringBuilder.build(getDoc(PrettyPrinterConfig.DEFAULT));
  }

  public String getShortMessage() {
    return DocStringBuilder.build(getShortHeaderDoc(PrettyPrinterConfig.DEFAULT));
  }

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
