package org.arend.frontend.repl;

import org.arend.prelude.GeneratedVersion;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintStream;

public class JLineCliRepl extends CommmonCliRepl {
  private final Terminal myTerminal;

  public JLineCliRepl(@NotNull Terminal terminal) {
    super(new PrintStream(terminal.output()));
    myTerminal = terminal;
  }

  public void runRepl() {
    var reader = LineReaderBuilder.builder()
        .appName(APP_NAME)
        .completer(new AggregateCompleter(
            new JLineKeywordCompleter(),
            new JLineExprCompleter(),
            new JLineCommandsCompleter()
        ))
        .terminal(myTerminal)
        .parser(new DefaultParser() {
          @Override
          public boolean isEscapeChar(CharSequence buffer, int pos) {
            return false;
          }
        }.escapeChars(new char[]{}))
        .build();
    while (true) {
      try {
        if (repl(reader::readLine, reader.readLine(prompt()))) break;
      } catch (UserInterruptException e) {
        println("[INFO] Ignored input: `" + e.getPartialLine() + "`.");
      } catch (EndOfFileException e) {
        break;
      }
    }
  }

  public static void main(String... args) {
    Terminal terminal;
    try {
      terminal = TerminalBuilder
          .builder()
          .encoding("UTF-8")
          .jansi(true)
          .build();
    } catch (IOException e) {
      System.err.println("[FATAL] Failed to create terminal: " + e.getLocalizedMessage());
      System.exit(1);
      return;
    }
    var repl = new JLineCliRepl(terminal);
    repl.println(APP_NAME + " " + GeneratedVersion.VERSION_STRING + ": https://arend-lang.github.io   :? for help");
    repl.initialize();
    repl.runRepl();
  }

}
