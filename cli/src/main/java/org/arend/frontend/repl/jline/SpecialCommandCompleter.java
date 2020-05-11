package org.arend.frontend.repl.jline;

import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class SpecialCommandCompleter implements Completer {
  private final List<String> commands;
  private final Completer completer;

  public SpecialCommandCompleter(@NotNull List<String> commands, @NotNull Completer completer) {
    this.commands = commands;
    this.completer = completer;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String lineText = line.line();
    int cursor = line.cursor();
    if (commands.stream().anyMatch(s -> lineText.startsWith(s) && cursor >= s.length())) {
      completer.complete(reader, line, candidates);
    }
  }
}
