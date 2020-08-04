package org.arend.ext.ui;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.NotNull;

public interface ArendConsole {
  void println(@NotNull Doc doc);

  @NotNull PrettyPrinterConfig getPrettyPrinterConfig();

  default void println(@NotNull String text) {
    println(DocFactory.text(text));
  }

  default void println(@NotNull CoreExpression expression) {
    println(DocFactory.termDoc(expression, getPrettyPrinterConfig()));
  }
}
