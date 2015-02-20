package main.java.com.jetbrains;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.Expression;
import main.java.com.jetbrains.term.typechecking.TypeCheckingException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, NotInScopeException, TypeCheckingException {
        InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
        ANTLRInputStream input = new ANTLRInputStream(stream);
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VcgrammarParser parser = new VcgrammarParser(tokens);
        ParseTree tree = parser.defs();
        BuildVisitor builder = new BuildVisitor();
        List<Definition> defs = (List<Definition>) builder.visit(tree);
        for (String var : builder.getUnknownVariables()) {
            System.err.print("Not in scope: " + var);
        }
        for (Definition def : defs) {
            if (def instanceof FunctionDefinition) {
                Expression term = ((FunctionDefinition)def).getTerm();
                def = new FunctionDefinition(def.getName(), def.getArguments(), def.getResultType().normalize(), term.normalize());
                if (builder.getUnknownVariables().isEmpty()) {
                    term.checkType(new ArrayList<Definition>(), def.getType());
                }
            }
            def.prettyPrint(System.out, new ArrayList<String>(), 0);
            System.out.println();
            System.out.println();
        }
    }
}
