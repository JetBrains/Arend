package org.arend.frontend.ui;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.ui.ArendConsole;
import org.arend.ext.ui.ArendSession;
import org.arend.ext.ui.ArendUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArendCliUI implements ArendUI, ArendConsole {
  @Override
  public @NotNull ArendSession newSession() {
    return new CliSession();
  }

  @Override
  public void showMessage(@Nullable String title, @NotNull String message) {
    System.out.println((title == null ? "" : title + ": ") + message);
  }

  @Override
  public void showErrorMessage(@Nullable String title, @NotNull String message) {
    System.err.println((title == null ? "" : title + ": ") + message);
  }

  @Override
  public @NotNull ArendConsole getConsole(@Nullable Object marker) {
    return this;
  }

  @Override
  public @NotNull PrettyPrinterConfig getPrettyPrinterConfig() {
    return PrettyPrinterConfig.DEFAULT;
  }

  @Override
  public void println(@NotNull Doc doc) {
    System.out.println(DocStringBuilder.build(doc));
  }
}
