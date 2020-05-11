package org.arend.frontend.repl;

import org.arend.repl.CommandHandler;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class JLineCommandsCompleter implements Completer {
  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (line.cursor() >= 1 && ':' == line.line().charAt(0) && line.wordIndex() <= 1) {
      for (var string : CommandHandler.INSTANCE.commandMap.keySet())
        candidates.add(new Candidate(":" + string));
    }
  }
}
