package org.arend.frontend.repl.jline;

import org.arend.repl.CommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public final class CommandsCompleter implements Completer {
  public static final @NotNull CommandsCompleter INSTANCE = new CommandsCompleter();

  private CommandsCompleter() {
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (line.cursor() >= 1 && ':' == line.line().charAt(0) && line.wordIndex() < 1) {
      for (var string : CommandHandler.INSTANCE.commandMap.keySet())
        candidates.add(new Candidate(":" + string));
    }
  }
}
