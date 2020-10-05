package org.arend.repl.action;

import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.repl.Repl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrettyPrintFlagCommand implements ReplCommand {
  public static final @NotNull PrettyPrintFlagCommand INSTANCE = new PrettyPrintFlagCommand();

  public static final @NotNull List<@NotNull String> AVAILABLE_OPTIONS = Stream
    .concat(
      Arrays.stream(PrettyPrinterFlag.values())
        .map(Enum::name)
        .map(String::toUpperCase),
      Arrays.stream(PrettyPrinterFlag.values())
        .map(Enum::name)
        .map(String::toLowerCase))
    .collect(Collectors.toList());

  @Contract(pure = true)
  private PrettyPrintFlagCommand() {
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
    return "Toggle a certain pretty printing flag";
  }

  @Override
  public @Nls @NotNull String help(@NotNull Repl api) {
    return "Toggle a certain pretty printing flag (currently " + api.prettyPrinterFlags + ".\n" +
      "Options available (case insensitive):\n" +
      Arrays.stream(PrettyPrinterFlag.values())
        .map(Enum::name)
        .collect(Collectors.joining(",\n")) + ".\n" +
      "If you do not pass an option, currently enabled options will be printed.";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
    if (line.isBlank()) {
      api.println("Flags: " + api.prettyPrinterFlags);
      return;
    }
    try {
      var flag = PrettyPrinterFlag.valueOf(line.toUpperCase(Locale.ROOT));
      if (api.prettyPrinterFlags.contains(flag)) {
        api.prettyPrinterFlags.remove(flag);
        api.println("[INFO] Disabled " + flag + ".");
      } else {
        api.prettyPrinterFlags.add(flag);
        api.println("[INFO] Enabled " + flag + ".");
      }
    } catch (IllegalArgumentException e) {
      api.eprintln("[ERROR] Invalid pretty-printing option " + line + ", available options: " + Arrays.toString(PrettyPrinterFlag.values()) + ".");
    }
  }
}
