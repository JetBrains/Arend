package org.arend.repl;

import org.arend.repl.action.ReplCommand;
import org.arend.util.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  /**
   * Split a command.
   * @param line Example: <code>:f a</code>
   * @return Example: <code>new Pair("f", "a")</code>
   */
  public static @NotNull Pair<@Nullable String, @NotNull String> splitCommand(@NotNull String line) {
    if (line.isBlank() || !line.startsWith(":")) return new Pair<>(null, line);
    int indexOfSpace = line.indexOf(' ');
    var command = indexOfSpace > 0 ? line.substring(1, indexOfSpace) : line.substring(1);
    var arguments = indexOfSpace > 0 ? line.substring(indexOfSpace + 1) : "";
    return new Pair<>(command, arguments.trim());
  }

  @Override
  public final void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> lineSupplier) throws QuitReplException {
    var command = splitCommand(line);
    if (command.proj1 == null) return;
    var replCommand = commandMap.get(command.proj1);
    if (replCommand != null) replCommand.invoke(command.proj2, api, lineSupplier);
    else {
      var suitableCommands = determineEntries(command.proj1).collect(Collectors.toList());
      if (suitableCommands.isEmpty())
        api.eprintln("[ERROR] Unrecognized command: " + command.proj1 + ".");
      else if (suitableCommands.size() >= 2)
        api.eprintln("[ERROR] Cannot distinguish among commands :"
          + suitableCommands.stream().map(Map.Entry::getKey).collect(Collectors.joining(", :"))
          + ", please be more specific.");
      else suitableCommands.get(0).getValue().invoke(command.proj2, api, lineSupplier);
    }
  }

  public @NotNull Stream<Map.Entry<String, ReplCommand>> determineEntries(@NotNull String command) {
    return commandMap.entrySet().stream().filter(entry -> entry.getKey().startsWith(command));
  }

  public final class HelpCommand implements ReplCommand {
    private HelpCommand() {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String description() {
      return "Show this message (`:? [command name]` to describe a command)";
    }

    @Override
    public @Nls @NotNull String help(@NotNull Repl api) {
      return "Command to show the help message.\n" +
        "Use `:? [command name]` to describe a command (like `:? ?` to show this message).";
    }

    @Override
    public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) {
      if (line.isBlank()) {
        noArg(api);
        return;
      }
      var replCommand = commandMap.get(line);
      if (replCommand == null) {
        api.eprintln("[ERROR] Cannot find command `:" + line + "`.");
        return;
      }
      api.println(replCommand.help(api));
    }

    private void noArg(@NotNull Repl api) {
      IntSummaryStatistics statistics = commandMap.keySet().stream()
        .mapToInt(String::length)
        .summaryStatistics();
      int maxWidth = Math.min(statistics.getMax(), 9) + 1;
      api.println("There are " + statistics.getCount() + " commands available.");
      for (var replCommand : commandMap.entrySet()) {
        var description = replCommand.getValue().description();
        String command = replCommand.getKey();
        api.println(":" + command + " ".repeat(maxWidth - command.length()) + description);
      }
      api.println("Note: to use an Arend symbol beginning with `:`, start the line with a whitespace.");
    }
  }
}
