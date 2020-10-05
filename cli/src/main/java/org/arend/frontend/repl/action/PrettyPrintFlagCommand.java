package org.arend.frontend.repl.action;

import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.frontend.repl.CommonCliRepl;
import org.arend.repl.QuitReplException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PrettyPrintFlagCommand implements CliReplCommand {
  public static final @NotNull PrettyPrintFlagCommand INSTANCE = new PrettyPrintFlagCommand();

  public static final @NotNull List<@NotNull String> AVAILABLE_OPTIONS = Arrays
    .stream(PrettyPrinterFlag.values())
    .map(Enum::name)
    .collect(Collectors.toList());

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
        api.println("Disabled " + flag + ".");
      } else {
        api.prettyPrinterFlags.add(flag);
        api.println("Enabled " + flag + ".");
      }
    } catch (IllegalArgumentException e) {
      api.eprintln("Invalid pretty-printing option " + line + ", available options: " + Arrays.toString(PrettyPrinterFlag.values()) + ".");
    }
  }
}
