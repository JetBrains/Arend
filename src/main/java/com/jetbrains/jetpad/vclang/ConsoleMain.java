package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.visitor.NormalizeVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleMain {

  public static void main(String[] args) throws IOException {
    InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
    ANTLRInputStream input = new ANTLRInputStream(stream);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    VcgrammarParser.DefsContext tree = parser.defs();
    BuildVisitor builder = new BuildVisitor();
    List<Definition> defs = builder.visitDefs(tree);
    Map<String, Definition> context = new HashMap<>();
    List<TypeCheckingError> errors = new ArrayList<>();
    for (Definition def : defs) {
      if (def instanceof FunctionDefinition) {
        def = def.checkTypes(context, errors);
        if (def != null) {
          def = new FunctionDefinition(def.getName(), def.getSignature(), ((FunctionDefinition) def).getTerm().normalize(NormalizeVisitor.Mode.NF));
          context.put(def.getName(), def);
        }
      }
      if (def != null) {
        def.prettyPrint(System.out, new ArrayList<String>(), 0);
        System.out.println();
        System.out.println();
      }
    }
    for (TypeCheckingError error : errors) {
      System.err.println(error.toString());
    }
  }
}
