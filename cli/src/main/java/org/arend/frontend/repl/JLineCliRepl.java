package org.arend.frontend.repl;

import org.arend.naming.reference.Referable;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JLineCliRepl extends CommonCliRepl {
  private final Terminal myTerminal;

  public JLineCliRepl(@NotNull Terminal terminal) {
    myTerminal = terminal;
  }

  @Override
  public void print(Object anything) {
    var writer = myTerminal.writer();
    writer.print(anything);
    writer.flush();
  }

  @Override
  public void println(Object anything) {
    myTerminal.writer().println(anything);
  }

  @Override
  public void println() {
    myTerminal.writer().println();
  }

  public void runRepl() {
    Path dir = Paths.get(System.getProperty("user.home")).resolve(FileUtils.USER_CONFIG_DIR);
    Path history = dir.resolve("history");
    try {
      // Assuming user.home exists
      if (Files.notExists(dir) || Files.isRegularFile(dir)) {
        Files.deleteIfExists(dir);
        Files.createDirectory(dir);
      }
      if (Files.notExists(history) || Files.isDirectory(history)) {
        Files.deleteIfExists(history);
        Files.createFile(history);
      }
    } catch (IOException e) {
      eprintln("[ERROR] Failed to load REPL history: " + e.getLocalizedMessage());
      history = null;
    }
    var reader = LineReaderBuilder.builder()
      .appName(APP_NAME)
      .variable(LineReader.HISTORY_FILE, history)
      .history(new DefaultHistory())
      .completer(new AggregateCompleter(
        scopeCompleter(),
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
        if (repl(reader.readLine(prompt()), reader::readLine)) break;
      } catch (UserInterruptException e) {
        println("[INFO] Ignored input: `" + e.getPartialLine() + "`.");
      } catch (EndOfFileException e) {
        break;
      }
    }
  }

  private @NotNull Completer scopeCompleter() {
    return (lineReader, line, candidates) -> {
      String word = line.word();
      var firstChar = word.isEmpty() ? '+' : word.charAt(0);
      if ("~!@#$%^&*-+=<>?/|:".indexOf(firstChar) > 0 || Character.isAlphabetic(firstChar)) {
        for (Referable referable : getInScopeElements())
          candidates.add(new Candidate(referable.getRefName()));
      }
    };
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
    repl.println(ASCII_BANNER);
    repl.println();
    repl.initialize();
    repl.runRepl();
  }

}
