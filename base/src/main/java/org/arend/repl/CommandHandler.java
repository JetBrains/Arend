package org.arend.repl;

import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class CommandHandler implements ReplHandler {
  public static final @NotNull CommandHandler INSTANCE = new CommandHandler();
  public static final @NotNull HelpCommand HELP_COMMAND_INSTANCE = INSTANCE.createHelpCommand();
  public final @NotNull Map<String, ReplCommand> commandMap = new LinkedHashMap<>();

  private @NotNull HelpCommand createHelpCommand() {
    return new HelpCommand();
  }

  @Override
  public final boolean isApplicable(@NotNull String line) {
    return line.startsWith(":");
  }

  @Override
  public final void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> lineSupplier) throws QuitReplException {
    int indexOfSpace = line.indexOf(' ');
    var command = indexOfSpace > 0 ? line.substring(1, indexOfSpace) : line.substring(1);
    var replCommand = commandMap.get(command);
    if (replCommand != null)
      replCommand.invoke(line.substring(indexOfSpace + 1), api, lineSupplier);
    else api.eprintln("[ERROR] Unrecognized command: " + command + ".");
  }

  public final class HelpCommand implements ReplCommand {
    private HelpCommand() {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return "Show this message";
    }

    @Override
    public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
      IntSummaryStatistics statistics = commandMap.keySet().stream()
        .mapToInt(String::length)
          .summaryStatistics();
      int maxWidth = Math.min(statistics.getMax(), 8) + 1;
      api.println("There are " + statistics.getCount() + " action(s) available.");
      for (var replCommand : commandMap.entrySet()) {
        var description = replCommand.getValue().description();
        if (description == null) continue;
        String command = replCommand.getKey();
        api.println(command + " ".repeat(maxWidth - command.length()) + description);
      }
    }
  }
}
