package org.arend.error;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocStringBuilder;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.DataContainer;
import org.arend.naming.reference.GlobalReferable;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.error.local.LocalError;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

import static org.arend.error.doc.DocFactory.*;

public class GeneralError {
  public final String message;
  public final @Nonnull Level level;

  public enum Level { INFO, WEAK_WARNING {
    @Override
    public String toString() {
      return "WARNING";
    }
  }, WARNING, GOAL, ERROR }

  public GeneralError(@Nonnull Level level, String message) {
    this.level = level;
    this.message = message;
  }

  public Object getCause() {
    return null;
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
    return null;
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

  public boolean isTypecheckingError() {
    return false;
  }

  public void forAffectedDefinitions(BiConsumer<GlobalReferable, GeneralError> consumer) {
    Object cause = getCause();
    if (cause instanceof GlobalReferable) {
      consumer.accept((GlobalReferable) cause, this);
    } else if (cause instanceof Collection) {
      for (Object elem : ((Collection) cause)) {
        if (elem instanceof GlobalReferable) {
          consumer.accept((GlobalReferable) elem, this);
        }
      }
    }
  }

  public boolean isSevere() {
    return getCause() == null;
  }
}
