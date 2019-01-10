import java.util.ArrayList;
import java.util.Stack;

public class Semantics {

    Stack<Function> callStack;

    State M(Program p) {
        Variable main = new Variable("main");
        ArrayList<Value> params = new ArrayList<Value>();
        callStack = new Stack<Function>();
        return M(p.functions, main, initialState(p.globals), params);
    }

    State M(Functions fs, Variable call, State globals, ArrayList<Value> params) {
        for (int i = 0; i < fs.members.size(); i++) {
            if (fs.members.get(i).name.toString().equals(call.toString())) {
                State locals = new State();
                Function f = new Function(fs.members.get(i).name, fs.members.get(i).type, fs.members.get(i).params, fs.members.get(i).locals, fs.members.get(i).body);
                callStack.push(f);
                for (int j = 0; j < f.params.size(); j++)
                    locals.put(f.params.get(j).v, params.get(j));
                locals.putAll(initialState(f.locals));
                if (call.toString().equals("main")) {
                    System.out.println("Entering " + call.toString() + " :");
                    System.out.println("\tGlobals and top frame:\n\t----------");
                    globals.display();
                    locals.display();
                    System.out.println("\t----------");
                } else {
                    System.out.println("Calling " + call.toString() + " :");
                    System.out.println("\tGlobals and top frame:\n\t----------");
                    globals.display();
                    locals.display();
                    System.out.println("\t<" + f.name + ", " + f.val + ">, ");
                    System.out.println("\t----------");
                }
                M(fs, f.body, globals, locals);
                if (call.toString().equals("main")) {
                    System.out.println("Leaving " + call.toString() + " :");
                    System.out.println("\tGlobals and top frame:\n\t----------");
                    globals.display();
                    locals.display();
                    System.out.println("\t----------");
                } else {
                    System.out.println("Returning " + call.toString() + " :");
                    System.out.println("\tGlobals and top frame:\n\t----------");
                    globals.display();
                    locals.display();
                    System.out.println("\t<" + f.name + ", " + f.val + ">, ");
                    System.out.println("\t----------");
                }
                callStack.pop();
                break;
            }
        }
        return globals;
    }

    State initialState(Declarations d) {
        State state = new State();
        Value intUndef = new IntValue();
        for (Declaration decl : d)
            state.put(decl.v, Value.mkValue(decl.t));
        return state;
    }


    State M(Functions fs, Statement s, State globals, State locals) {
        if (s instanceof Skip) return M(fs, (Skip) s, globals, locals);
        if (s instanceof Assignment) return M(fs, (Assignment) s, globals, locals);
        if (s instanceof Call) {
            ArrayList<Value> params = new ArrayList<Value>();
            for (int i = 0; i < ((Call) s).params.size(); i++) {
                params.add(M(fs, ((Call) s).params.get(i), globals, locals));
            }
            return M(fs, ((Call) s).name, globals, params);
        }
        if (s instanceof Conditional) return M(fs, (Conditional) s, globals, locals);
        if (s instanceof Loop) return M(fs, (Loop) s, globals, locals);
        if (s instanceof Block) return M(fs, (Block) s, globals, locals);
        if (s instanceof Return) return M(fs, (Return) s, globals, locals);
        throw new IllegalArgumentException("should never reach here");
    }

    State M(Functions fs, Skip s, State globals, State locals) {
        return locals;
    }

    State M(Functions fs, Assignment a, State globals, State locals) {
        if (locals.containsKey(a.target))
            return locals.onion(a.target, M(fs, a.source, globals, locals));
        else
            return globals.onion(a.target, M(fs, a.source, globals, locals));
    }

    State M(Functions fs, Block b, State globals, State locals) {
        State state = new State();
        for (Statement s : b.members)
            state = M(fs, s, globals, locals);
        return state;
    }

    State M(Functions fs, Conditional c, State globals, State locals) {
        if (M(fs, c.test, globals, locals).boolValue())
            return M(fs, c.thenbranch, globals, locals);
        else
            return M(fs, c.elsebranch, globals, locals);
    }

    State M(Functions fs, Loop l, State globals, State locals) {
        if (M(fs, l.test, globals, locals).boolValue())
            return M(fs, l, M(fs, l.body, globals, locals), locals);
        else return locals;
    }

    State M(Functions fs, Return r, State globals, State locals) {
        callStack.get(callStack.size() - 1).val = M(fs, r.result, globals, locals);
        return locals;
    }

    Value applyBinary(Operator op, Value v1, Value v2) {
        StaticTypeCheck.check(!v1.isUndef() && !v2.isUndef(), "reference to undef value");
        if (op.val.equals(Operator.INT_PLUS))
            return new IntValue(v1.intValue() + v2.intValue());
        if (op.val.equals(Operator.INT_MINUS))
            return new IntValue(v1.intValue() - v2.intValue());
        if (op.val.equals(Operator.INT_TIMES))
            return new IntValue(v1.intValue() * v2.intValue());
        if (op.val.equals(Operator.INT_DIV))
            return new IntValue(v1.intValue() / v2.intValue());

        if (op.val.equals(Operator.INT_LT))
            return new BoolValue(v1.intValue() < v2.intValue());
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue() <= v2.intValue());
        if (op.val.equals(Operator.INT_EQ))
            return new BoolValue(v1.intValue() == v2.intValue());
        if (op.val.equals(Operator.INT_NE))
            return new BoolValue(v1.intValue() != v2.intValue());
        if (op.val.equals(Operator.INT_GT))
            return new BoolValue(v1.intValue() > v2.intValue());
        if (op.val.equals(Operator.INT_GE))
            return new BoolValue(v1.intValue() >= v2.intValue());

        if (op.val.equals(Operator.FLOAT_PLUS))
            return new FloatValue(v1.floatValue() + v2.floatValue());
        if (op.val.equals(Operator.FLOAT_MINUS))
            return new FloatValue(v1.floatValue() - v2.floatValue());
        if (op.val.equals(Operator.FLOAT_TIMES))
            return new FloatValue(v1.floatValue() * v2.floatValue());
        if (op.val.equals(Operator.FLOAT_DIV))
            return new FloatValue(v1.floatValue() / v2.floatValue());

        if (op.val.equals(Operator.FLOAT_LT))
            return new BoolValue(v1.floatValue() < v2.floatValue());
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue() <= v2.floatValue());
        if (op.val.equals(Operator.FLOAT_EQ))
            return new BoolValue(v1.floatValue() == v2.floatValue());
        if (op.val.equals(Operator.FLOAT_NE))
            return new BoolValue(v1.floatValue() != v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GT))
            return new BoolValue(v1.floatValue() > v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GE))
            return new BoolValue(v1.floatValue() >= v2.floatValue());

        if (op.val.equals(Operator.CHAR_LT))
            return new BoolValue(v1.charValue() < v2.charValue());
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue() <= v2.charValue());
        if (op.val.equals(Operator.CHAR_EQ))
            return new BoolValue(v1.charValue() == v2.charValue());
        if (op.val.equals(Operator.CHAR_NE))
            return new BoolValue(v1.charValue() != v2.charValue());
        if (op.val.equals(Operator.CHAR_GT))
            return new BoolValue(v1.charValue() > v2.charValue());
        if (op.val.equals(Operator.CHAR_GE))
            return new BoolValue(v1.charValue() >= v2.charValue());

        if (op.val.equals(Operator.BOOL_EQ))
            return new BoolValue(v1.boolValue() == v2.boolValue());
        if (op.val.equals(Operator.BOOL_NE))
            return new BoolValue(v1.boolValue() != v2.boolValue());
        if (op.val.equals(Operator.AND))
            return new BoolValue(v1.boolValue() && v2.boolValue());
        if (op.val.equals(Operator.OR))
            return new BoolValue(v1.boolValue() || v2.boolValue());

        if (op.val.equals(Operator.PLUS))
            return new IntValue(v1.intValue() + v2.intValue());
        if (op.val.equals(Operator.MINUS))
            return new IntValue(v1.intValue() - v2.intValue());
        if (op.val.equals(Operator.TIMES))
            return new IntValue(v1.intValue() * v2.intValue());
        if (op.val.equals(Operator.DIV))
            return new IntValue(v1.intValue() / v2.intValue());

        throw new IllegalArgumentException("should never reach here");
    }

    Value applyUnary(Operator op, Value v) {
        StaticTypeCheck.check(!v.isUndef(), "reference to undef value");
        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue());
        else if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue());
        else if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue());
        else if (op.val.equals(Operator.I2F))
            return new FloatValue((float) (v.intValue()));
        else if (op.val.equals(Operator.F2I))
            return new IntValue((int) (v.floatValue()));
        else if (op.val.equals(Operator.C2I))
            return new IntValue((int) (v.charValue()));
        else if (op.val.equals(Operator.I2C))
            return new CharValue((char) (v.intValue()));
        throw new IllegalArgumentException("should never reach here");
    }

    Value M(Functions fs, Expression e, State globals, State locals) {
        if (e instanceof Value)
            return (Value) e;
        if (e instanceof Variable) {
            if (locals.containsKey(e))
                return (Value) (locals.get(e));
            else
                return (Value) (globals.get(e));
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            return applyBinary(b.op, M(fs, b.term1, globals, locals), M(fs, b.term2, globals, locals));
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            return applyUnary(u.op, M(fs, u.term, globals, locals));
        }
        if (e instanceof Call) {
            for (int i = 0; i < fs.members.size(); i++) {
                if (fs.members.get(i).name.equals(((Call) e).name)) {
                    ArrayList<Value> params = new ArrayList<Value>();
                    for (int j = 0; j < fs.members.get(i).params.size(); j++) {
                        params.add(M(fs, ((Call) e).params.get(j), globals, locals));
                    }
                    return M2(fs, fs.members.get(i).name, globals, params);
                }
            }
        }
        throw new IllegalArgumentException("should never reach here");
    }

    Value M2(Functions fs, Variable call, State globals, ArrayList<Value> params) {
        Value result = null;
        for (int i = 0; i < fs.members.size(); i++) {
            if (fs.members.get(i).name.toString().equals(call.toString())) {
                State locals = new State();
                Function f = new Function(fs.members.get(i).name, fs.members.get(i).type, fs.members.get(i).params, fs.members.get(i).locals, fs.members.get(i).body);
                callStack.push(f);
                for (int j = 0; j < f.params.size(); j++)
                    locals.put(f.params.get(j).v, params.get(j));
                locals.putAll(initialState(f.locals));

                System.out.println("Calling " + call.toString() + " :");
                System.out.println("\tGlobals and top frame:\n\t----------");
                globals.display();
                locals.display();
                System.out.println("\t<" + f.name + ", " + f.val + ">, ");
                System.out.println("\t----------");

                M(fs, f.body, globals, locals);
                result = f.val;

                System.out.println("Returning " + call.toString() + " :");
                System.out.println("\tGlobals and top frame:\n\t----------");
                globals.display();
                locals.display();
                System.out.println("\t<" + f.name + ", " + f.val + ">, ");
                System.out.println("\t----------");
                callStack.pop();

                break;
            }
        }
        return result;
    }

    public static void main(String args[]) {
        String filename = "src\\Test Programs\\recFib.cpp";
        Parser parser = new Parser(new Lexer(filename));
        System.out.println("Begin parsing... " + filename + "\n");
        Program prog = parser.program();
        prog.display();
        System.out.println("\nBegin type checking... " + filename);
        System.out.print("Globals = ");
        TypeMap globals = StaticTypeCheck.typing(prog.globals);
        globals.display();
        System.out.println();
        StaticTypeCheck.V(prog);
        System.out.println("No type errors\n");
        Program out = TypeTransformer.T(prog, globals);
//        System.out.println("Transformed Abstract Syntax Tree\n");
//        out.display();
        System.out.println("Begin interpreting... " + filename + "\n");
        Semantics semantics = new Semantics();
        State state = semantics.M(out);
        System.out.println("\nFinal State");
        System.out.println("\tGlobals and top frame:\n\t----------");
        state.display();
        System.out.println("\t----------");
    }
}
