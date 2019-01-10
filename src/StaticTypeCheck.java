import java.util.*;

public class StaticTypeCheck {

    public static TypeMap typing(Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di : d)
            map.put(di.v, di.t);
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test) return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V(Declarations d) {
        for (int i = 0; i < d.size() - 1; i++)
            for (int j = i + 1; j < d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
    }

    public static void V(Program p) {
        V(p.globals);
        V(p.functions, typing(p.globals));
    }

    public static void V(Functions fs, TypeMap globals) {
        for (int i = 0; i < fs.members.size(); i++) {
            System.out.print("Function " + fs.members.get(i).name + " =" + "\n{");
            TypeMap tm = new TypeMap();
            tm.putAll(globals);
            tm.putAll(typing(fs.members.get(i).params));
            tm.putAll(typing(fs.members.get(i).locals));
            Iterator<Variable> iter = tm.keySet().iterator();
            for (int k = 0; k < tm.size(); k++) {
                System.out.print("\t");
                Variable v = iter.next();
                Type t = tm.get(v);
                if (i + 1 != tm.size())
                    System.out.println("<" + v + ", " + t + ">");
            }
            for (int j = 0; j < fs.members.size(); j++) {
                System.out.print("\t");
                System.out.print("<" + fs.members.get(j).name + ", " + fs.members.get(j).type + ", ");
                typing(fs.members.get(j).params).display();
                if (j + 1 == fs.members.size())
                    System.out.print(">");
                else
                    System.out.println(">");
                tm.put(fs.members.get(j).name, fs.members.get(j).type);
            }
            V(fs.members.get(i).body, tm);
            System.out.println(" }\n");
        }
    }

    public static Type typeOf(Expression e, TypeMap tm) {
        if (e instanceof Value) return ((Value) e).type;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, tm) == Type.FLOAT)
                    return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            if (u.op.NotOp()) return (Type.BOOL);
            else if (u.op.NegateOp()) return typeOf(u.term, tm);
            else if (u.op.intOp()) return (Type.INT);
            else if (u.op.floatOp()) return (Type.FLOAT);
            else if (u.op.charOp()) return (Type.CHAR);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            return typeOf(((Call) e).name, tm);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Expression e, TypeMap tm) {
        if (e instanceof Value)
            return;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            V(b.term1, tm);
            V(b.term2, tm);
            if (b.op.ArithmeticOp())
                check(typ1 == typ2 && (typ1 == Type.INT || typ1 == Type.FLOAT), "type error for " + b.op);
            else if (b.op.RelationalOp())
                check(typ1 == typ2, "type error for " + b.op);
            else if (b.op.BooleanOp())
                check(typ1 == Type.BOOL && typ2 == Type.BOOL, b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }

        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Type typ = typeOf(u.term, tm);
            V(u.term, tm);
            if (u.op.NotOp())
                check(typ == Type.BOOL, "type error for " + u.op);
            else if (u.op.NegateOp())
                check(((typ == Type.INT) || (typ == Type.FLOAT)), "type error for " + u.op);
            else if (u.op.intOp() || u.op.floatOp() || u.op.charOp()) ;
            else
                throw new IllegalArgumentException("should never reach here" + u.op);
            return;
        }
    }

    public static void V(Statement s, TypeMap tm) {
        if (s == null)
            throw new IllegalArgumentException("AST error: null statement");
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment) s;
            check(tm.containsKey(a.target), " undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = (Type) tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check(srctype == Type.INT, "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check(srctype == Type.CHAR, "mixed mode assignment to " + a.target);
                else
                    check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }

        if (s instanceof Call) {
            Call f = (Call) s;
            check(tm.containsKey(f.name), " undefined function: " + f.name);
            return;
        }

        if (s instanceof Block) {
            Block b = (Block) s;
            for (int i = 0; i < b.members.size(); i++)
                V(b.members.get(i), tm);
            return;
        }

        if (s instanceof Conditional) {
            Conditional c = (Conditional) s;
            V(c.test, tm);
            V(c.thenbranch, tm);
            V(c.elsebranch, tm);
            return;
        }

        if (s instanceof Loop) {
            Loop l = (Loop) s;
            V(l.test, tm);
            V(l.body, tm);
            return;
        }

        if (s instanceof Return) {
            Return r = (Return) s;
            V(r.name, tm);
            V(r.result, tm);
            return;
        }

        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        String filename = "src\\Test Programs\\recFib.cpp";
        Parser parser = new Parser(new Lexer(filename));
        System.out.println("Begin parsing... " + filename + "\n");
        Program prog = parser.program();
        prog.display();
        System.out.println("\nBegin type checking... " + filename);
        System.out.print("Globals = ");
        TypeMap globals = typing(prog.globals);
        globals.display();
        System.out.println();
        V(prog);
        System.out.println("No type errors\n");
    }

}

