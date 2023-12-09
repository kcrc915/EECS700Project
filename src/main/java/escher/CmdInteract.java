package escher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CmdInteract {
    public static void printTable(List<List<String>> elements, int spacing, Set<Integer> alignRightCols, int indent) {
        int rows = elements.size();
        int cols = elements.get(0).size();
        int[] colWidths = new int[cols];
        for (int c = 0; c < cols; c++) {
            int maxWidth = 0;
            for (int r = 0; r < rows; r++) {
                int width = elements.get(r).get(c).length();
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
            colWidths[c] = maxWidth;
        }
        for (List<String> row : elements) {
            for (int i = 0; i < indent; i++) {
                System.out.print(" ");
            }
            for (int c = 0; c < row.size(); c++) {
                String str = row.get(c);
                if (alignRightCols.contains(c)) {
                    for (int i = 0; i < colWidths[c] - str.length(); i++) {
                        System.out.print(" ");
                    }
                    System.out.print(str);
                    for (int i = 0; i < spacing; i++) {
                        System.out.print(" ");
                    }
                } else {
                    System.out.print(str);
                    for (int i = 0; i < colWidths[c] + spacing - str.length(); i++) {
                        System.out.print(" ");
                    }
                }
            }
            System.out.println();
        }
    }
}


