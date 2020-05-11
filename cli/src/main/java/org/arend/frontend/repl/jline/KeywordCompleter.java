package org.arend.frontend.repl.jline;

import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class KeywordCompleter implements Completer {
  public static final @NotNull List<@NotNull String> arendKeywords = List.of(
      "open", "import", "using", "as", "hiding", "func", "sfunc", "lemma", "cons",
      "classifying", "noclassifying", "field", "property", "override",
      "infix", "infixl", "infixr", "fix", "fixl", "fixr",
      "Prop", "where", "with", "use", "cowith", "elim", "new", "pi", "sigma", "lam", "let", "lets",
      "in", "case", "scase", "data", "class", "record", "module", "meta", "extends",
      "return", "coerce", "instance", "truncated",
      "lp", "lh", "oo", "suc", "max", "level", "oo-Type",
      "Set", "Type", "this",
      "eval", "peval"
  );

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (line.word().startsWith("\\")) {
      for (var arendKeyword : arendKeywords) {
        candidates.add(new Candidate("\\" + arendKeyword));
      }
    }
  }
}
