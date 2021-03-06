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
package nl.detoren.ijc.ui.model;

import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.table.AbstractTableModel;
import nl.detoren.ijc.data.wedstrijden.Groepswedstrijden;
import nl.detoren.ijc.data.wedstrijden.Wedstrijd;
import nl.detoren.ijc.data.wedstrijden.Wedstrijden;
import nl.detoren.ijc.ui.control.IJCController;

/**
 *
 * @author Leo van der Meulen
 */
public class WedstrijdModel extends AbstractTableModel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IJCController controller = null;
    private JComponent component;
    private int groepID;

    private String[] columnNames = {" ", "Wit", "", "Zwart", "Uitslag"};

    public WedstrijdModel() {
        this(0, null);
    }

    public WedstrijdModel(int g, JComponent c) {
        component = c;
        groepID = g;
        init();
    }

    public void init() {
        controller = IJCController.getInstance();
    }

    public void setGroep(int groepID) {
        this.groepID = groepID;
    }

    public int getGroep() {
        return groepID;
    }

    @Override
    public int getRowCount() {
        Wedstrijden ws = controller.getWedstrijden();
        Groepswedstrijden gws = ws.getGroepswedstrijdenNiveau(groepID);
        ArrayList<Wedstrijd> w = gws.getWedstrijden();
        return w.size();
        //return controller.getWedstrijden().getGroepswedstrijdenNiveau(groepID).getWedstrijden().size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        if (column >= 0 && column < columnNames.length) {
            return columnNames[column];
        } else {
            return "";

        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Wedstrijd ws = controller.getWedstrijden().getGroepswedstrijdenNiveau(groepID).getWedstrijden().get(rowIndex);
        switch (columnIndex) {
            case 0:
                return new Integer(ws.getId());
            case 1:
                return ws.getWit().getNaam();
            case 2:
                return "-";
            case 3:
                return ws.getZwart().getNaam();
            default:
                switch(ws.getUitslag()) {
                    case 0:
                        return "0-0";
                    case 1:
                        return "1-0";
                    case 2:
                        return "0-1";
                    case 3:
                        return "�-�";
                    default:
                        return "0-0";
                }
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
            case 0:
                return Integer.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 4;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        fireTableCellUpdated(row, col);
        component.repaint();
    }

}
