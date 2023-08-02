package org.arend.repl;

import org.arend.repl.action.AliasableCommand;
import org.arend.repl.action.ReplCommand;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
  public boolean isApplicable(@NotNull String line) {
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
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> lineSupplier) throws QuitReplException {
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
      else
        suitableCommands.get(0).getValue().invoke(command.proj2, api, lineSupplier);
    }
  }

  public @NotNull Stream<Map.Entry<String, ReplCommand>> determineEntries(@NotNull String command) {
    return commandMap.entrySet().stream().filter(entry -> entry.getKey().startsWith(command));
  }

  public final class HelpCommand extends AliasableCommand {
    private HelpCommand() {
      super(new ArrayList<>());
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
      IntSummaryStatistics statistics = commandMap.entrySet()
          .stream()
          .filter(it -> !(it.getValue() instanceof AliasableCommand))
          .map(Map.Entry::getKey)
          .mapToInt(String::length)
          .summaryStatistics();
      int maxWidth = statistics.getMax() + 1;
      api.println("Enter Arend expression, statement (e. g. an \\import-command) or a REPL command");
      api.println("There are " + statistics.getCount() + " commands available.");
      var set = new HashSet<AliasableCommand>();
      for (var replCommand : commandMap.entrySet()) {
        var commandValue = replCommand.getValue();
        var description = commandValue.description();
        String command = replCommand.getKey();
        if (commandValue instanceof AliasableCommand) {
          if (!set.contains(commandValue)) {
            var aliasableCommand = (AliasableCommand) commandValue;
            set.add(aliasableCommand);
            String prefix = aliasableCommand.aliases
                .stream()
                .map(it -> ":" + it)
                .collect(Collectors.joining(", "));
            api.println(prefix + " ".repeat(1 + Math.max(0, maxWidth - prefix.length())) + description);
          }
        } else
          api.println(":" + command + " ".repeat(maxWidth - command.length()) + description);
      }
      api.println("Note: to use an Arend symbol beginning with `:`, start the line with a whitespace.");
    }
  }
}
