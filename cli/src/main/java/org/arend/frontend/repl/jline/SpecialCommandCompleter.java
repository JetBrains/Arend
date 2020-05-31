package org.arend.frontend.repl.jline;

import org.arend.repl.CommandHandler;
import org.arend.repl.action.ReplCommand;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Map;

public class SpecialCommandCompleter implements Completer {
  private final Class<? extends ReplCommand> commandClass;
  private final Completer completer;

  public SpecialCommandCompleter(
    @NotNull Class<? extends ReplCommand> commandClass,
    @NotNull Completer completer
  ) {
    this.commandClass = commandClass;
    this.completer = completer;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    int cursor = line.cursor();
    var command = CommandHandler.splitCommand(line.line());
    if (command.proj1 != null && cursor > command.proj1.length() && CommandHandler.INSTANCE
      .determineEntries(command.proj1)
      .map(Map.Entry::getValue)
      .anyMatch(commandClass::isInstance)
    ) completer.complete(reader, line, candidates);
  }
}
