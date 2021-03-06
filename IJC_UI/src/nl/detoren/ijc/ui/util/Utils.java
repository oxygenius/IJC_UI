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
               System.out.print(grid[r][c] + ";");
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

    public static int[][] add2DArrays(double mf1, int A[][], double mf2, int B[][]){
    	// Just for [X+1][X] arrays! with index in first row.
    	int C[][] = new int[A.length][A[0].length];
    	for (int i=0;i<A.length;i++) {
    		for (int j=0;j<A[0].length;j++) {
    			if (j==0) {
    				C[i][j]=(int) (A[i][j]+B[i][j]);
    			} else {
    				C[i][j]=(int) (mf1*A[i][j]+mf2*B[i][j]);
    			}
    		}
    	}
    	return C;
    }

    public static int triagonalsum(int A[][]){
    	// Just for [X][X] arrays! 
    	int sum = 0;
    	for (int i=0;i<A.length;i++) {
    		for (int j=Math.max(0, i-1);j<Math.min(i+2, A.length);j++) {
    				// System.out.print("index1 = " + i + ", index2 = " + (j) + " = " + A[i][j] + "\n ");
    				sum += A[i][j];
    		}
    	}
    	return sum;
    }

    public static int triagonalsum(int A[][], int indexrow){
    	// Just for [X+1][X] arrays! with index (indexrow=1) in first row.
    	int sum = 0;
    	for (int i=0;i<A.length;i++) {
    		for (int j=Math.max(0, i-1);j<Math.min(i+2, A.length);j++) {
    				 // System.out.print("index1 = " + i + ", index2 = " + (indexrow+j) + " = " + A[i][indexrow+j] + "\n ");
    				sum += A[i][indexrow+j];
    		}
    	}
    	return sum;
    }
    
    public static boolean containing(int[] haystack, int needle) {
    	for(int hay: haystack){
    		if(hay == needle)
    			return true;
    	}
    	return false;
    }
    
    public static int[][] removerowandcolumnfrom2D(int A[][], int[] trio, int indexrow) {
    	// Just for cubic equal size arrays!
    	int C[][] = new int[A.length-3][A[0].length-3];
    	int p = 0;
    	for (int i=0;i<A.length;i++) {
    		int q = 0;
			if (!(Utils.containing(trio,A[i][indexrow-1]))) {
				C[p][q]=A[i][0];
				q++;
				for (int j=1;j<A[0].length;j++) {
					if (!(Utils.containing(trio,A[j-1][indexrow-1]))) {
						C[p][q]=A[i][j];
						q++;
					}
   				}
				p++;
   			}
    	}
    	return C;
    }
}
