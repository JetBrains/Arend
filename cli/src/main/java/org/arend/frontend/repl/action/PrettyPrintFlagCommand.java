package org.arend.frontend.repl.action;

import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.QuitReplException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

public class PrettyPrintFlagCommand implements CliReplCommand {
  public static final @NotNull PrettyPrintFlagCommand INSTANCE = new PrettyPrintFlagCommand();

  @Contract(pure = true)
  private PrettyPrintFlagCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Toggle a certain pretty printing flag";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull CommonCliRepl api, @NotNull Supplier<@NotNull String> scanner) {
    if (line.isBlank()) {
      api.println("Flags: " + api.prettyPrinterFlags);
      return;
    }
    try {
      var flag = PrettyPrinterFlag.valueOf(line.toUpperCase(Locale.ROOT));
      if (api.prettyPrinterFlags.contains(flag)) {
        api.prettyPrinterFlags.remove(flag);
        api.println("Enabled " + flag + ".");
      } else {
        api.prettyPrinterFlags.add(flag);
        api.println("Disabled " + flag + ".");
      }
    } catch (IllegalArgumentException e) {
      api.eprintln("Invalid pretty-printing option " + line + ", available options: " + Arrays.toString(PrettyPrinterFlag.values()) + ".");
    }
  }
}
