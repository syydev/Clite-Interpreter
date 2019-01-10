import java.util.*;

public class State extends HashMap<Variable, Value> {

    public State() {
    }

    public State(Variable key, Value val) {
        put(key, val);
    }

    public State onion(Variable key, Value val) {
        put(key, val);
        return this;
    }

    public State onion(State t) {
        for (Variable key : t.keySet())
            put(key, t.get(key));
        return this;
    }

    public void display() {
        Iterator<Variable> iter = this.keySet().iterator();
        for (int i = 0; i < this.size(); i++) {
            Variable v = iter.next();
            Value val = this.get(v);
            System.out.println("\t<" + v + ", " + val + ">, ");
        }
    }

}
