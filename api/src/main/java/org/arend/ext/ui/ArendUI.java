package org.arend.ext.ui;

import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Provides a way for the extension to interact with the user.
 */
public interface ArendUI {
  @NotNull ArendSession newSession();

  void showMessage(@Nullable String title, @NotNull String message);

  void showErrorMessage(@Nullable String title, @NotNull String message);

  @NotNull EnumSet<PrettyPrinterFlag> getPrettyPrinterFlags();

  void println(@NotNull Doc doc);


  default @NotNull PrettyPrinterConfig getPrettyPrinterConfig() {
    return new PrettyPrinterConfig() {
      @Override
      public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
        return getPrettyPrinterFlags();
      }
    };
  }

  default void println(@NotNull String text) {
    println(DocFactory.text(text));
  }

  default void println(@NotNull CoreExpression expression) {
    println(DocFactory.termDoc(expression, getPrettyPrinterConfig()));
  }
}
