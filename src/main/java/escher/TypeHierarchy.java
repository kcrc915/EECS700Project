package escher;

import java.util.HashSet;
import java.util.Set;


class TypeHierarchy {
    interface TypeTree {
        Set<TypeNode> children = new HashSet<>();

        default void printTree(int tab) {
            String tabS = " ".repeat(tab);
            printTree(0, (d, s) -> {
                System.out.print(tabS.repeat(d));
                System.out.print(s);
            });
        }

        void printTree(int depth, Printer printer);

        void connectToParent(TypeHierarchy.TypeNode typeNode);

        interface Printer {
            void print(int depth, String s);
        }
    }



    static class TypeNode implements TypeTree {
        private final Set<TypeTree> parents = new HashSet<>();



        public void connectToParent(TypeTree p) {
            p.children.add(this);
            parents.add(p);
        }

        public void deleteConnectionToParent(TypeTree p) {
            p.children.remove(this);
            parents.remove(p);
        }

        @Override
        public void printTree(int depth, Printer printer) {
            printer.print(depth, "-  \n");
            children.forEach(child -> child.printTree(depth + 1, printer));
        }

        @Override
        public void connectToParent(TypeNode typeNode) {

        }
    }

    /**
     * try to insert this typeNode into this TypeTree if this type is not already in the tree
     *
     * @return whether the node has been inserted
     */

}


