package escher;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * using "import DSL._" to write terms more easily
 */
public class DSL {

    public static class ComponentFromString {
        private String name;
        public ComponentFromString(String name) {
            this.name = name;
        }
        /** allowing us to write <i>"Component(n, args*)"</i> as <i>"n $ (args*)"</i> */

    }
    public static ComponentFromString componentFromString(String name) {
        return new ComponentFromString(name);
    }
    public static Var v(String name) {
        return new Var(name);
    }

    public static If if_(Term condition, Term thenBranch, Term elseBranch) {
        return new If(condition, thenBranch, elseBranch);
    }
    public static Var var(String name) {
        return new Var(name);
    }
    public static TVar tyVar(int id) {
        return new TVar(id);
    }
    public static TFixedVar tyFixVar(int id) {
        return new TFixedVar(id);
    }
    public static ValueInt intConversion(int i) {
        return new ValueInt(i);
    }
    public static valueBool boolConversion(boolean b) {
        return new valueBool(b);
    }
    public static ValueTree binaryTreeConversion(BinaryTree<termvalue> t) {
        return new ValueTree(t);
    }
    public static < A > ValueTree binaryTreeConversion(BinaryTree< A > t, Function< A, termvalue> conv) {
        return new ValueTree(t.map(conv));
    }
    public static ValuePair pairConversion(termvalue a, termvalue b) {
        return new ValuePair(a, b);
    }
    public static < A, B > ValuePair pairConversion2(A a, B b, Function< A, termvalue> convA, Function< B, termvalue> convB) {
        return new ValuePair(convA.apply(a), convB.apply(b));
    }

    public static ValueList listValue(termvalue... terms) {
        return new ValueList(Arrays.asList(terms));
    }
    public static final TInt tyInt = TInt.of();
    public static final TBool tyBool = TBool.of();

}


