package escher;

import java.util.HashSet;
import java.util.Set;

/** Useless yet */
class TypeHierarchy {
    private final RootNode typeTree = new RootNode();
}

/** Useless yet */
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

        interface Printer {
            void print(int depth, String s);
        }
    }

    static class RootNode implements TypeTree {
        @Override
        public void printTree(int depth, Printer printer) {
            printer.print(depth, "- Root\n");
        }
    }

    static class TypeNode implements TypeTree {
        private final Type ty;
        private final Set<TypeTree> parents = new HashSet<>();

        public TypeNode(Type ty) {
            this.ty = ty;
        }

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
            printer.print(depth, "- " + ty + "\n");
            children.forEach(child -> child.printTree(depth + 1, printer));
        }
    }

    /**
     * try to insert this typeNode into this TypeTree if this type is not already in the tree
     *
     * @return whether the node has been inserted
     */
    static boolean insertTypeNode(TypeTree tree, TypeNode typeNode) {
        Type ty = typeNode.ty;
        boolean isDirectChild = true;
        for (TypeTree child : tree.children) {
            if (ty instanceof child.ty.getClass()) {
                if (child.ty instanceof ty.getClass()) {
                    return false;
                } else {
                    // ty is proper child of child.ty
                    insertTypeNode(child, typeNode);
                    isDirectChild = false;
                }
            } else {
                if (child.ty instanceof ty.getClass()) {
                    child.deleteConnectionToParent(tree);
                    child.connectToParent(typeNode);
                }
            }
        }
        if (isDirectChild) {
            typeNode.connectToParent(tree);
        }
        return true;
    }
}


