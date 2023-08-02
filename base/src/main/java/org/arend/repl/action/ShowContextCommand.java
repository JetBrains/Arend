package org.arend.repl.action;

import org.arend.core.definition.Definition;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.repl.QuitReplException;
import org.arend.repl.Repl;
import org.arend.term.NamespaceCommand;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ShowContextCommand implements ReplCommand {
  public static final @NotNull ShowContextCommand INSTANCE = new ShowContextCommand();
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Override
  public @NotNull String description() {
    return "Prints Repl context";
  }

  @Override
  public void invoke(@NotNull String line, @NotNull Repl api, @NotNull Supplier<@NotNull String> scanner) throws QuitReplException {
    StringBuilder builder = new StringBuilder();
    for (Statement statement: api.statements) {
      NamespaceCommand command = statement.getNamespaceCommand();
      Group group = statement.getGroup();
      Referable referable = group != null ? group.getReferable() : null;
      Concrete.ReferableDefinition definition = referable instanceof ConcreteLocatedReferable ? ((ConcreteLocatedReferable) referable).getDefinition() : null;
      if (command != null) command.prettyPrint(builder, PrettyPrinterConfig.DEFAULT);
      if (definition != null) definition.prettyPrint(builder, PrettyPrinterConfig.DEFAULT);
      builder.append("\n");
    }
    api.print(builder.toString());
  }
}
