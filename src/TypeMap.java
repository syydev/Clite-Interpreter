import java.util.*;

public class TypeMap extends HashMap<Variable, Type> {

    public void display() {
        System.out.print("{");
        Iterator<Variable> iter = this.keySet().iterator();
        for (int i = 0; i < this.size(); i++) {
            Variable v = iter.next();
            Type t = this.get(v);
            if (i + 1 != this.size())
                System.out.print("<" + v + ", " + t + ">, ");
            else
                System.out.print("<" + v + ", " + t + ">");
        }
        System.out.print("}");
    }
}
