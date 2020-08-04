package org.arend.frontend.ui;

import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocStringBuilder;
import org.arend.ext.ui.ArendSession;
import org.arend.ext.ui.ArendUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ArendCliUI implements ArendUI {
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
  public @NotNull EnumSet<PrettyPrinterFlag> getPrettyPrinterFlags() {
    return PrettyPrinterConfig.DEFAULT.getExpressionFlags();
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
