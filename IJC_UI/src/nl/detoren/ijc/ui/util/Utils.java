/**
 * Copyright (C) 2016 Leo van der Meulen
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3.0
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * See: http://www.gnu.org/licenses/gpl-2.0.html
 *  
 * Problemen in deze code:
 * - ... 
 * - ...
 */

package nl.detoren.ijc.ui.util;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.table.TableColumn;
import java.util.ArrayList;

/**
 *
 * @author Leo van der Meulen
 */
public class Utils {
    
    public static void fixedComponentSize(Component c, int width, int height) {
        c.setMinimumSize(new Dimension(width, height));
        c.setMinimumSize(new Dimension(width, height));
        c.setPreferredSize(new Dimension(width, height));
        c.setSize(new Dimension(width, height));
    }
    
    public static void fixedColumnSize(TableColumn c, int width) {
        c.setMinWidth(width);
        c.setMaxWidth(width);
    }
    
  //Displays a 2d array in the console, one line per row.
    public static void printMatrix(ArrayList<ArrayList<Integer>> grid) {
        for(int r=0; r<grid.size(); r++) {
           for(int c=0; c<grid.get(r).size(); c++)
               System.out.print(grid.get(r).get(c) + " ");
           System.out.println();
        }
    }

    public static void printMatrix(int grid[][]) {
        for(int r=0; r<grid.length; r++) {
           for(int c=0; c<grid[0].length; c++)
               System.out.print(grid[r][c] + " ");
           System.out.println();
        }
    }

    public static void printMatrix(int grid[]) {
        for(int r=0; r<grid.length; r++) {
               System.out.print(grid[r] + " \n");
        }
    }

    public static int[][] add2DArrays(int A[][], int B[][]){
    	// Just for cubic equal size arrays!
    	int C[][] = new int[A.length][A.length];
    	for (int i=0;i<A.length;i++) {
    		for (int j=0;j<B.length;j++) {
    			C[i][j]=A[i][j]+B[i][j];
    		}
    	}
    	return C;
    }    
}
