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
 */
package nl.detoren.ijc.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import nl.detoren.ijc.data.groepen.Groep;
import nl.detoren.ijc.data.groepen.Groepen;
import nl.detoren.ijc.data.groepen.Speler;

/**
 * Leest alle gegevens van de spelers uit een invoerbestand
 * @author Leo van der Meulen
 */
public class GroepenReader {

	private final static Logger logger = Logger.getLogger(GroepenReader.class.getName());
	//logger.log(Level.INFO, "Logbericht");

    public GroepenReader() {

    }

    /**
     * Lees groepen uit standaardbestand, nl uitslag.txt
     * @return De ingelezen spelers verdeeld over de groepen
     */
    public Groepen leesGroepen() {
        return leesGroepen("uitslag.txt");
    }
    
    /**
     * Lees groepen uit het gespecificeerde textbestand
     * @param bestandsnaam Naam van het bestand dat ingelezen moet worden
     * @return De ingelezen spelers verdeeld over de groepen
     */
    public Groepen leesGroepenJSON(String bestandsnaam) {
		try {
			logger.log(Level.INFO, "Lezen groepen in JSON formaat uit : " + bestandsnaam);
			Gson gson = new Gson();
			BufferedReader br = new BufferedReader(new FileReader(bestandsnaam));
			Groepen groepen = gson.fromJson(br, Groepen.class);
			logger.log(Level.INFO, "Groepen gelezen met periode " +  groepen.getPeriode() + " en ronde " + groepen.getRonde());
	        groepen.setRonde(groepen.getRonde()+1);
	        if (groepen.getRonde() > 8) {
	        	groepen.setRonde(1);
	        	groepen.setPeriode(groepen.getPeriode()+1);
	        	if (groepen.getPeriode() > 4) groepen.setPeriode(1);
	        }
			logger.log(Level.INFO, "Volgende periode " +  groepen.getPeriode() + " en ronde " + groepen.getRonde());
	        return groepen;
		} catch (IOException e) {
			logger.log(Level.INFO, "Lezen groepen in JSON formaat mislukt " +  e.getMessage());
		}
		return null;
    }
    
    /**
     * Lees groepen uit het gespecificeerde textbestand
     * @param bestandsnaam Naam van het bestand dat ingelezen moet worden
     * @return De ingelezen spelers verdeeld over de groepen
     */
    public Groepen leesGroepen(String bestandsnaam) {
		logger.log(Level.INFO, "Lezen groepen in TXT formaat uit : " + bestandsnaam);

        // Lees het volledige bestand in naar een String array
        String[] stringArr = leesBestand(bestandsnaam);

        // Lees iedere groep in en voeg deze toe aan de verzameling groepen
        Groepen groepen = new Groepen();
        int ronde = leesRonde(stringArr, "STAND NA");
        int periode = leesPeriode(stringArr, "STAND NA");
        ronde += 1;
        if (ronde > 8) {
        	ronde = 1;
        	periode++;
        	if (periode > 4) periode = 1;
        }
		logger.log(Level.INFO, "Periode " + periode  + " en ronde " + ronde + " als speelronde");
        groepen.setRonde(ronde);
        groepen.setPeriode(periode);
        groepen.addGroep(leesGroep(stringArr, "KEIZERGROEP", Groep.KEIZERGROEP));
        groepen.addGroep(leesGroep(stringArr, "KONINGSGROEP", Groep.KONINGSGROEP));
        groepen.addGroep(leesGroep(stringArr, "DAMEGROEP", Groep.DAMEGROEP));
        groepen.addGroep(leesGroep(stringArr, "TORENGROEP", Groep.TORENGROEP));
        groepen.addGroep(leesGroep(stringArr, "LOPERGROEP", Groep.LOPERGROEP));
        groepen.addGroep(leesGroep(stringArr, "PAARDENGROEP", Groep.PAARDENGROEP));
        groepen.addGroep(leesGroep(stringArr, "PIONNENGROEP", Groep.PIONNENGROEP));
		logger.log(Level.INFO, "Groepen gelezen, speel periode " +  groepen.getPeriode() + " en ronde " + groepen.getRonde());
        return groepen;
    }

    /**
     * Lees een bestand in en retourneer dit als Strings.
     * @param bestandsnaam
     * @return array of strings met bestandsinhoud
     */
    private String[] leesBestand(String bestandsnaam) {
        List<String> list = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(bestandsnaam));
            String str;
            while ((str = in.readLine()) != null) {
                list.add(str);
            }
            in.close();
            return list.toArray(new String[0]);
        } catch (IOException e) {
            System.out.println("Exception :" + e);
        }
        return null;
    }

    /**
     * Lees alle spelers van een groep in
     * Op de start regel meot de groepsnaam staan vanaf kolom 30 (of hoger)
     * De eerste speler staat twee regels verder
     * @param data String array met invoerbestand
     * @param token Tekst om te zoeken waar de specifieke groep begint
     * @param type Groepsniveau
     * @return Groep van het gespecificeerde niveau met al zijn spelers
     */
    private Groep leesGroep(String[] data, String token, int type) {
		logger.log(Level.INFO, "Zoeken naar tolen \'" +  token + "\' en type " + type);
        Groep groep = new Groep(type);
        // Zoek regel met token
        boolean found = false;
        int index = 0;
        while (!found && index < data.length) {
            found = (data[index].length() > 40) && (data[index].toUpperCase().substring(30).contains(token));
            index++;
        }
        // Lees spelers (begint 3 regels na het vinden van de groepsnaam)
        // Index is hier ��n groter dan de index waar he token is gevonden
        index += 2;
		logger.log(Level.INFO, "Spelers groep beginnen op regel " + index);
        // Zolang er een punt in de regel staat, is er nog een speler gevonden
        while ((index < data.length) && data[index].contains(".")) {
        	Speler s= genereerSpeler(data[index++], type);
    		logger.log(Level.FINE, "Spelerregel : " + data[index-1]);
    		logger.log(Level.FINE, "Speler      : " + s.toPrintableString());
            groep.addSpeler(s);
        }
        return groep;
    }
    /**
     * Bepaal de gegevens van een speler op basis van de String
     * met de spelerbeschrijving.
     * @param desc De string waarin de speler gegevens zitten
     * @return Speler conversie van string naar speler
     */
    private Speler genereerSpeler(String desc, int groep) {
        Speler speler = new Speler();
        System.out.println(desc);
        // ID
        speler.setId(getIntegerDeel(desc, 0, 2));
        // Naam
        speler.setNaam(getStringDeel(desc, 4, 31));
        // Initialen
        speler.setInitialen(getStringDeel(desc, 36, 2));
        // afwezigheidspunt
        speler.setAfwezigheidspunt(getStringDeel(desc, 39, 1).equals("#"));
        // Wit voorkeur
        String tmp = getStringDeel(desc, 41, 1);
        switch (tmp) {
            case "w":
                speler.setWitvoorkeur(getIntegerDeel(desc, 42, 1));
                break;
            case "z":
                speler.setWitvoorkeur(-1 * getIntegerDeel(desc, 42, 1));
                break;
            default:
                speler.setWitvoorkeur(0);
                break;
        }
        // Rating
        speler.setRating(getIntegerDeel(desc, 45, 4));
        // Tegenstanders
        String[] tgn = new String[4];
        tgn[0] = getStringDeel(desc, 53, 3);
        tgn[1] = getStringDeel(desc, 56, 3);
        tgn[2] = getStringDeel(desc, 59, 3);
        tgn[3] = getStringDeel(desc, 62, 3);
        speler.setTegenstanders(tgn);
        // Punten. Extra spatie nodig om gegarandeerd te kunnen lezen
        speler.setPunten(getIntegerDeel(desc + " ", 65, 5));
        // Eigen groep
        speler.setGroep(groep);
        return speler;
    }
    
    /**
     * Lees ronde uit bestand. Deze staat op positie 8 of 9 in de regel met 
     * de tekst STAND NA (case incensitive)
     * @param data
     * @param token
     * @return
     */
    private int leesRonde(String[] data, String token) {
        // Zoek regel met token
        boolean found = false;
        int index = 0;
        while (!found && index < data.length) {
            found = (data[index].length() > 40) && (data[index].toUpperCase().contains(token));
            index++;
        }
        index -= 1; 										// index is na doorlopen loop 1 te hoog
        return getIntegerDeel(data[index], 8, 2);			// Ronde staat in kolom 8,9 van deze regel
    }
    
    /**
     * Lees periode uit bestand. Deze staat op positie 18 of 19 in de regel met 
     * de tekst STAND NA (case incensitive)
     * @param data
     * @param token
     * @return
     */
    private int leesPeriode(String[] data, String token) {
        // Zoek regel met token
        boolean found = false;
        int index = 0;
        while (!found && index < data.length) {
            found = (data[index].length() > 40) && (data[index].toUpperCase().contains(token));
            index++;
        }
        index -= 1;
        return getIntegerDeel(data[index], 19, 2);		// Ronde staat in kolom 19,19 van deze regel
    }

    /**
     * Retourneer een deelstring op basis van offset en lengte.
     * Retourneert een lege string indien de deelstring niet bepaald kan worden
     * @param input invoerstring
     * @param offset offset waar substring moet worden bepaald
     * @param length aantal in te lezen karakters
     * @return de deelstring
     */
    private String getStringDeel(String input, int offset, int length) {
        if ((input != null) && ((offset + length) <= input.length())) {
            return input.substring(offset, offset + length);
        }
        return "";
    }

    /**
     * Retourneer het getal (integer) uit de tekst op basis van offset en lengte.
     * Retourneert '0' indien het getal niet bepaald kan worden
     * @param input invoerstring
     * @param offset offset waar substring moet worden bepaald
     * @param length aantal in te lezen karakters
     * @return de deelstring
     */
    private int getIntegerDeel(String input, int offset, int length) {
        String str = getStringDeel(input, offset, length);
        str = str.trim();
        str = (str.startsWith("0")) ? str.substring(1) : str;
        return !str.equals("") ? Integer.decode(str) : 0;
    }
}
