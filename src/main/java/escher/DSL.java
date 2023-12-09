package escher;
import scala.collection.immutable;
/**
 * using "import DSL._" to write terms more easily
 */
public class DSL {
  import Term.*;
  import Type.*;
    public static class ComponentFromString {
        private String name;
        public ComponentFromString(String name) {
            this.name = name;
        }
        /** allowing us to write <i>"Component(n, args*)"</i> as <i>"n $ (args*)"</i> */
        public Component $ (Term... args) {
            return new Component(name, args.toIndexedSeq());
        }
    }
    public static ComponentFromString componentFromString(String name) {
        return new ComponentFromString(name);
    }
    public static Var v(String name) {
        return new Var(name);
    }
    public static Component c(String name, Term... args) {
        return new Component(name, args.toIndexedSeq());
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
    public static ValueBool boolConversion(boolean b) {
        return new ValueBool(b);
    }
    public static ValueTree binaryTreeConversion(BinaryTree< TermValue > t) {
        return new ValueTree(t);
    }
    public static < A > ValueTree binaryTreeConversion(BinaryTree< A > t, Function< A, TermValue > conv) {
        return new ValueTree(t.map(conv));
    }
    public static ValuePair pairConversion(TermValue a, TermValue b) {
        return new ValuePair(a, b);
    }
    public static < A, B > ValuePair pairConversion2(A a, B b, Function< A, TermValue > convA, Function< B, TermValue > convB) {
        return new ValuePair(convA.apply(a), convB.apply(b));
    }
    public static < A > ValueList listConversion(List< A > list, Function< A, TermValue > convA) {
        return new ValueList(list.map(convA));
    }
    public static ValueList listValue(TermValue... terms) {
        return new ValueList(Arrays.asList(terms));
    }
    public static final TApply tyInt = TInt.of();
    public static final TApply tyBool = TBool.of();
    public static TApply tyList(Type param) {
        return TList.of(param);
    }
    public static TApply tyMap(Type kt, Type vt) {
        return TMap.of(kt, vt);
    }
    public static TApply tyTree(Type param) {
        return TTree.of(param);
    }
    public static TApply tyPair(Type t1, Type t2) {
        return TPair.of(t1, t2);
    }
    public static IS< TermValue > argList(TermValue... termValues) {
        return termValues.toIndexedSeq();
    }
}


