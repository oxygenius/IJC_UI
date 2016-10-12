/**
 * Copyright (C) 2016 Leo van der Meulen, Lars Dam
 * 
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
 * - TODO Bij oneven aantal spelers in de hoogste groep wordt er een volledig trio ingepland -> Handmatig aanpassen   
 * - TODO Afmelden van speler die is doorgeschoven, werkt nog niet. -> Workaround: Delete in afwezigheidstabel
 * - TODO Parametriseren van Fuzzy waarden
 */
package nl.detoren.ijc.ui.control;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.detoren.ijc.data.groepen.Groep;
import nl.detoren.ijc.data.groepen.Groepen;
import nl.detoren.ijc.data.groepen.Speler;
import nl.detoren.ijc.data.wedstrijden.Groepswedstrijden;
import nl.detoren.ijc.data.wedstrijden.Serie;
import nl.detoren.ijc.data.wedstrijden.Wedstrijd;
import nl.detoren.ijc.data.wedstrijden.Wedstrijden;
import nl.detoren.ijc.ui.util.Utils;
import nl.detoren.ijc.ui.util.minimizetriagonal;

/**
 * Deelt de groepen in op basis van aanwezigheid en methode. Mogelijke methoden zijn 
 * 1. Zonder doorschuiven 
 * 2. 3 of 4 spelers schuiven een groep omhoog. 
 * Eindresultaat is altijd een groep met even spelers
 *
 * @author Leo van der Meulen
 */
public class GroepenIndeler {

    public static final int VASTEGROEP = 0;
    public static final int DOORSCHUIVEN = 1;
    public static final int AANTALDOORSCHUIVEN = 4;

    private final static Logger logger = Logger.getLogger(GroepenIndeler.class.getName());

    /**
     * Maak de groepsindeling voordat de wedstrijden worden bepaald. Spelers die afwezig zijn, worden uit de speellijst
     * verwijderd. Indien van toepassing, worden 3 of 4 spelers doorgescheven naar een hogere groep. De hogere groep
     * eindigt altijd met een even aantal spelers. Bij oneven spelers worden er dus 3 doorgeschoven en bij even spelers
     * mogen er 4 doorschuiven.
     *
     * @param aanwezigheidsGroepen Overzicht spelers per groen met aanwezigheidsinfo
     * @return de wedstrijdgroepen
     */
    public Groepen maakGroepsindeling(Groepen aanwezigheidsGroepen) {
    	logger.log(Level.INFO, "Maken groepsindeling voor alle groepen");
        // Er wordt een nieuwe groepen gemaakt, welke stapsgewijs gevuld gaat worden.
        int ronde = aanwezigheidsGroepen.getRonde();
        int periode = aanwezigheidsGroepen.getPeriode();
    	logger.log(Level.INFO, "Groepsindeling voor periode " + periode + ", ronde " + ronde);
        Groepen wedstrijdGroepen = new Groepen();
        wedstrijdGroepen.setPeriode(periode);
        wedstrijdGroepen.setRonde(ronde);
        // Eerst bepalen we de aanwezige spelers
        // Groepen worden gekopieerd maar zonder de afwezige spelers
        for (Groep groep : aanwezigheidsGroepen.getGroepen()) {
        	logger.log(Level.INFO, "Indeling voor groep " + groep.getNaam());
            Groep wedstrijdGroep = new Groep();
            wedstrijdGroep.setNiveau(groep.getNiveau());
            for (Speler speler : groep.getSpelers()) {
                if (speler.isAanwezig()) {
                	logger.log(Level.FINE, "Toevoegen aan wedstrijdgroep van speler" + speler.getNaam());
                    wedstrijdGroep.addSpeler(new Speler(speler));
                }
            }
            wedstrijdGroepen.addGroep(wedstrijdGroep);
        }
        // indien van toepassing, schuif maximaal 4 spelers door
        if (bepaalDoorschuiven(periode, ronde)) {
        	logger.log(Level.INFO, "Er wordt doorgeschoven, schuif door");
            doorschuiven(wedstrijdGroepen, aanwezigheidsGroepen);

        }
        // Hernummer alle groepen om overzicht te behouden
        // en dubbele nummers in een groep te voorkomen
    	logger.log(Level.INFO, "Hernummeren van spelers");
        wedstrijdGroepen.hernummerGroepen();
        return wedstrijdGroepen;
    }

    /**
     * Werk de groepsindeling van één groep bij voordat de wedstrijden worden bepaald. Spelers die afwezig zijn, worden 
     * uit de speellijst verwijderd. Indien van toepassing, worden 3 of 4 spelers doorgescheven naar een hogere groep. 
     * De hogere groep eindigt altijd met een even aantal spelers. Bij oneven spelers worden er dus 3 doorgeschoven en 
     * bij even spelers mogen er 4 doorschuiven.
     * @param aanwezigheidsGroepen Overzicht spelers per groep met aanwezigheidsinfo
     * @param wedstrijdGroepen Huidige wedstrijdgroepen
     * @param groepID Specificeert de groep die ge-update moet worden
     * @return de wedstrijdgroepen
     */
    public Groepen maakGroepsindeling(Groepen aanwezigheidsGroepen, Groepen wedstrijdGroepen, int groepID) {
    	logger.log(Level.INFO, "Maken groepsindeling voor groep" + aanwezigheidsGroepen.getGroepById(groepID).getNaam());
    	Groep aanwezigheidsGroep = aanwezigheidsGroepen.getGroepById(groepID);
    	Groep origineleWedstrijdGroep = wedstrijdGroepen.getGroepById(groepID);
    	// Zoek spelers uit deze groep die doorgeschoven zijn naar een hogere groep
    	ArrayList<Speler> doorgeschoven = new ArrayList<>();
    	Groep groepHoger = wedstrijdGroepen.getGroepById(groepID+1);
    	if (groepHoger != null) {
        	logger.log(Level.FINE, "Bepalen doorgeschoven spelers in deze groep");
    		doorgeschoven = groepHoger.getSpelersMetAnderNiveau();
        	logger.log(Level.FINE, "Aantal doorgeschoven spelers : " + doorgeschoven.size());    		
    	} 	
    	// Creeer nieuwe groep
    	// Neem alle aanwezige spelers hier in op, behalve degene die al doorgeschoven
    	// zijn naaar een hogere groep
    	Groep nieuweWedstrijdGroep = new Groep();
        nieuweWedstrijdGroep.setNiveau(aanwezigheidsGroep.getNiveau());
        for (Speler speler : aanwezigheidsGroep.getSpelers()) {
            if (speler.isAanwezig() && !groepBevat(doorgeschoven, speler)) {
            	logger.log(Level.FINE, "Toevoegen aan wedstrijdgroep van speler" + speler.getNaam());
                nieuweWedstrijdGroep.addSpeler(new Speler(speler));
            }
        }
        // Kopieer doorgescheven spelers uit oude lijst
        for (Speler speler : origineleWedstrijdGroep.getSpelers()) {
        	if (speler.getGroep() != origineleWedstrijdGroep.getNiveau()) {
            	logger.log(Level.FINE, "Toevoegen aan wedstrijdgroep van doorgeschoven speler" + speler.getNaam());
        		nieuweWedstrijdGroep.addSpeler(speler);
        	}
        }
    	logger.log(Level.INFO, "Aantal spelers in wedstrijdgroep: " + nieuweWedstrijdGroep.getAantalSpelers());    		
        nieuweWedstrijdGroep.renumber();
    	wedstrijdGroepen.updateGroep(nieuweWedstrijdGroep, groepID);
    	return wedstrijdGroepen;
    }

    public boolean groepBevat(ArrayList<Speler> doorgeschoven, Speler speler) {
    	logger.log(Level.INFO, "Speler : " + speler + ", in lijst met grootte " + doorgeschoven.size() );    		
    	for (Speler s : doorgeschoven) {
    		if (s.gelijkAan(speler)) return true;
    	}
    	return false;
    }
    /**
     * Schuif spelers door. Laatste speler wordt alleen doorgeschoven indien dit tot een even
     * aantal spelers in de nieuwe groep leidt.
     * @param wedstrijdGroepen
     * @param aanwezigheidsGroepen 
     */
    private void doorschuiven(Groepen wedstrijdGroepen, Groepen aanwezigheidsGroepen) {
        int aantal = bepaalAantalDoorschuiven(aanwezigheidsGroepen.getPeriode(), aanwezigheidsGroepen.getRonde());
    	logger.log(Level.INFO, "Aantal door te schuiven spelers "  + aantal);    		
        // Doorloop hoogste groep tot Ã©Ã©n na laagste groep. In de laagste groep
        // kunnen geen spelers inschuiven
        ArrayList<Groep> groepen = wedstrijdGroepen.getGroepen();
        for (int i = 0; i < groepen.size() - 1; ++i) {
        	logger.log(Level.FINE, "Doorschuiven van groep "  + groepen.get(i).getNaam() + " naar " + groepen.get(i).getNaam());    		
            ArrayList<Speler> naarGroep = groepen.get(i).getSpelers();
            ArrayList<Speler> vanGroep = groepen.get(i + 1).getSpelers();
            for (int j = 1; j <= aantal; ++j) {
                Speler s = groepen.get(i + 1).getSpelerByID(j);
            	logger.log(Level.FINE, "Speler : " + (s != null ? s.getNaam() : "null"));    		
                if ((s != null) && s.isAanwezig()) {
                    if ((j == aantal) && (aantal == 1)) {
                        // Alleen doorschuiven als speler 1 niet meer ingehaald kan worden
                        Speler s2 = groepen.get(i + 1).getSpelerByID(j);
                        if (s.getPunten() > (s2.getPunten() + 5)) {
                        	logger.log(Level.FINE, "Speler doorgeschoven, niet meer in te halen ");    		
                            naarGroep.add(new Speler(s));
                            vanGroep.remove(s);

                        }
                    } else if (j == aantal) {
                        if (naarGroep.size() % 2 != 0) {
                        	logger.log(Level.FINE, "Speler doorgeschoven, laatste doorschuiver maar door om even aantal ");    		
                            naarGroep.add(new Speler(s));
                            vanGroep.remove(s);
                        }
                    } else {
                    	logger.log(Level.FINE, "Speler doorgeschoven, niet laatste dus altijd");    		
                        naarGroep.add(new Speler(s));
                        vanGroep.remove(s);

                    }
                }

            }
        }
    }

    /**
     * Op basis van periode en ronde gegevens wordt bepaald of er wel of niet wordt doorgeschoven
     *
     * @param periode Huidige periode
     * @param ronde Huidige ronde
     * @return true als er met doorschuiven wordt gespeeld
     */
    public boolean bepaalDoorschuiven(int periode, int ronde) {
        return ronde >= 4;
    }

    public int bepaalAantalDoorschuiven(int periode, int ronde) {
        if (ronde >= 4) {
            if (ronde < 8) {
                return 4;
            } else {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Bepaal het minimale verschil tussen twee spelers die tegen elkaar spelen
     *
     * @param groep De groep
     * @param periode Periode
     * @param ronde Ronde in de periode
     * @param serie serie wedstrijden binnen de ronde
     * @return
     */
    public int bepaalMinimaalVerschil(Groep groep, int periode, int ronde, int serie) {
        int aantal = groep.getSpelers().size();
    	logger.log(Level.INFO, "Periode " + periode + " ronde " + ronde + " serie " + serie);    		
    	logger.log(Level.INFO, "groep " + groep.getNaam() + " met grootte " + aantal);  
    	int resultaat;
        if (groep.getNiveau() == Groep.KEIZERGROEP) {
            resultaat = ((periode == 1) && (ronde == 1) && (serie == 1)) ? (aantal / 2) : 1;
        	logger.log(Level.FINE, "Keizergroep: Minimaal verschil = " + resultaat);    		
        } else if (ronde > 1) {
        	logger.log(Level.FINE, "Ronde > 1: Minimaal verschil = " + serie);    		
            resultaat = serie;
        } else {
            resultaat = (serie == 1 ? (aantal / 2) : (serie == 2 ? 1 : 2));
        	logger.log(Level.FINE, "Ronde = 1 : Minimaal verschil = " + resultaat);    		
        }
        String log = groep.getNaam() + "in periode "+ periode + ", ronde " + ronde;
        log += ", serie " + serie + "-> minimaal verschil = " + resultaat;
    	logger.log(Level.INFO, log);  
        return resultaat;
    }

    /**
     * Bepaal het het aantal series dat tijdens een ronde wordt gespeeld. Dit is afhankelijk van de groep, 
     * de periode en
     * de ronde.
     *
     * @param groep Niveau van de groep
     * @param periode Periode
     * @param ronde Ronde in de periode
     * @return
     */
    public int bepaalAantalSeries(int groep, int periode, int ronde) {
    	logger.log(Level.INFO, "Vaststellen aantal te spelen series");    		
        if (groep == Groep.KEIZERGROEP) {
            if ((periode == 1) && (ronde == 1)) {
            	logger.log(Level.INFO, "Keizergroep, periode 1 en ronde 1. # series = 2");    		
                return 2;
            }
        	logger.log(Level.INFO, "Keizergroep, niet (periode 1 en ronde 1). # series = 1");    		
            return 1;
        }
        if ((periode == 1) && (ronde == 1)) {
        	logger.log(Level.INFO, "Niet Keizergroep, periode 1 en ronde 1. # series = 3");    		
            return 3;
        }
    	logger.log(Level.INFO, "Niet Keizergroep, niet (periode 1 en ronde 1). # series = 2");    		
        return 2;
    }

    /**
     Maak het wedstrijdschema voor een avond
     @param groepen
     @param periode
     @param ronde
     @return 
     */
    public Wedstrijden maakWedstrijdschema(Groepen groepen) {
    	int periode = groepen.getPeriode();
    	int ronde = groepen.getRonde();
    	logger.log(Level.INFO, "Maken wedstrijden voor periode " + periode + " ronde " + ronde);    		
        Wedstrijden wedstrijden = new Wedstrijden();        
        System.out.println("--------------------------------------------------------------");
        for (Groep groep : groepen.getGroepen()) {
            System.out.println(groep.toPrintableString());
        }
        System.out.println("-------------------------------------------------------------");
        for (Groep groepOrg : groepen.getGroepen()) {
        	logger.log(Level.INFO, "Maken wedstrijden voor groep " + groepOrg.getNaam());    		
            Groepswedstrijden gws = maakWedstrijdenVoorGroep(periode, ronde, groepOrg);
            wedstrijden.addGroepswedstrijden(gws);
        	logger.log(Level.INFO, "Aantal wedstrijden " + gws.getWedstrijden().size());    		
        }
        wedstrijden.setPeriode(periode);
        wedstrijden.setRonde(ronde);
        return wedstrijden;
    }
    
    /**
     * Update wedstrijden voor één groep. Wedstrijden voor alle andere groepen blijven
     * ongewijzigd.
     * @param wedstrijden Huidige wedstrijden voor alle groepen
     * @param wedstrijdgroepen Huidige wedstrijdgroepen voor all groepen
     * @param groepID ID van groep om opnieuw te bepalen
     * @return update van wedstrijden met nieuwe wedstrijden voor specifieke groep
     */
    public Wedstrijden updateWedstrijdschema(Wedstrijden wedstrijden, Groepen wedstrijdgroepen, int groepID) {
    	int periode = wedstrijdgroepen.getPeriode();
    	int ronde = wedstrijdgroepen.getRonde();
    	logger.log(Level.INFO, "Update wedstrijden voor groep " + groepID + " periode " + periode + " ronde " + ronde);    		
        Wedstrijden wedstrijdenNieuw = new Wedstrijden(); 
        wedstrijdenNieuw.setPeriode(periode);
        wedstrijdenNieuw.setRonde(ronde);
        for (Groepswedstrijden gw : wedstrijden.getGroepswedstrijden()) {
        	if (gw.getNiveau() == groepID) {
        		Groep wsGroep = wedstrijdgroepen.getGroepById(groepID);
        		Groepswedstrijden nieuw = maakWedstrijdenVoorGroep(periode, ronde, wsGroep);
        		wedstrijdenNieuw.addGroepswedstrijden(nieuw);
        	} else {
        		wedstrijdenNieuw.addGroepswedstrijden(gw);
        	}
        }
        return wedstrijdenNieuw;
    }

    /**
     * Bepaal voor een groep de te spelen wedstrijden
     * @param periode
     * @param ronde
     * @param wedstrijdgroep
     * @return
     */
	private Groepswedstrijden maakWedstrijdenVoorGroep(int periode, int ronde, Groep wedstrijdgroep) {
    	logger.log(Level.INFO, "Bepalen wedstrijden voor groep " + wedstrijdgroep.getNaam() + " periode " + periode + " ronde " + ronde);    		
		// Maak clone van de Groep om ongewenste updates te voorkomen
		Groep groep = new Groep();
		groep.setNiveau(wedstrijdgroep.getNiveau());
		for (Speler s : wedstrijdgroep.getSpelers()) {
	    	logger.log(Level.FINE, "Toevoegen van speler " + s.getNaam());    		
		    groep.addSpeler(new Speler(s));
		}
		if ((groep.getNiveau() == Groep.KEIZERGROEP) && (ronde < 7) && (ronde > 1)) {
			// Sorteer keizergroep op rating voor indeling indien ronde = 2,3,4,5 of 6
			groep.sorteerRating();
		}
		// Maak wedstrijden
		Groepswedstrijden gws = new Groepswedstrijden();
		gws.setNiveau(groep.getNiveau());
		int speelrondes = bepaalAantalSeries(groep.getNiveau(), periode, ronde);
    	logger.log(Level.INFO, "Aantal speelrondes " + speelrondes);    		

		// Trucje voor 5 speler in een wedstrijdgroep:
		// ALS 5 spelers in 2 ronden, dupliceer spelers naar 10 en plan
		// maar 1 ronde in. Dit heeft het juiste aantal wedstrijden tot gevolg
		if (groep.getAantalSpelers() == 5 && speelrondes == 2 ) {
			speelrondes=1;
	    	logger.log(Level.INFO, "Vijf spelers met 2 rondes dus spelers verdubbelen en maar één serie");    		
		    for (Speler s : wedstrijdgroep.getSpelers()) {
		        groep.addSpeler(new Speler(s));
		    }
		    // plan 1 round and duplicate players
		}
		// Introductie Fuzzy Logic
		//
		
		// Loop pas gebruiken als ook update is toegevoegd
		//for (int i = 0; i < speelrondes; ++i) {
			gws.setFuzzyMatrix(MaakFuzzyMatrix(wedstrijdgroep, 1));
			logger.log(Level.INFO, "FuzzyMatrix created.");
			int order[] = new int[wedstrijdgroep.getAantalSpelers()];
			for (int k = 0;k<wedstrijdgroep.getAantalSpelers();k++){
				order[k]=k;
			}
	        System.out.print("Trigonalization of Matrix\n");
	        minimizetriagonal triagonal = new minimizetriagonal();
	        triagonal.setA(gws.getFuzzyMatrix());
	        triagonal.setOrder(order);
	        triagonal.setIterations(10);
	        triagonal.Iterminimizetriagonal();
	        order = minimizetriagonal.getOrder();
	        int[][] tri = minimizetriagonal.getA();
	        System.out.print("Deze groep " + wedstrijdgroep.getNaam() + " heeft " + tri.length + " spelers.\n");
	        int trioloc = minimizetriagonal.gettrio(tri);
	        if (trioloc == 0) {
	        	System.out.print("Geen trio in deze groep.\n");
	        } else { 
		        System.out.print("Trio rond Speler op plaats " + (trioloc+1) + ".\n");
		    }
	        System.out.print("Minimize matrix is\n");
	        Utils.printMatrix(tri);
	        System.out.print("Order vector is\n");
	        Utils.printMatrix(order);
	        Serie s = new Serie();
	        int wedstrijdnr = 1;
	        if (trioloc == 0) {
	        	for (int k = 0;k<=wedstrijdgroep.getAantalSpelers()-1;k+=2){
					Speler s1 = wedstrijdgroep.getSpelerByID(order[k]+1); // Speler wit
					Speler s2 = wedstrijdgroep.getSpelerByID(order[k+1]+1); // Speler zwart
	        	   	Wedstrijd w = new Wedstrijd(wedstrijdnr, s1, s2, 0);
	        		s.addWedstrijd(w, true);
	        		wedstrijdnr++;
		        	System.out.printf("Wedstrijd met spelers op plaats %d en %d \n",order[k], order[k+1] );
				}	        	
	        } else {
	        	for (int k = 0;k<trioloc-2;k+=2){
					Speler s1 = wedstrijdgroep.getSpelerByID(order[k]+1); // Speler wit
					Speler s2 = wedstrijdgroep.getSpelerByID(order[k+1]+1); // Speler zwart
	        	   	Wedstrijd w = new Wedstrijd(wedstrijdnr, s1, s2, 0);
	        		s.addWedstrijd(w, true);
	        		wedstrijdnr++;
		        	System.out.printf("Wedstrijd met spelers op plaats %d en %d \n", order[k], order[k+1] );
				}
	        	for (int k = trioloc+2;k<=wedstrijdgroep.getAantalSpelers()-1;k+=2){
					Speler s1 = wedstrijdgroep.getSpelerByID(order[k]+1); // Speler wit
					Speler s2 = wedstrijdgroep.getSpelerByID(order[k+1]+1); // Speler zwart
					Wedstrijd w = new Wedstrijd(wedstrijdnr, s1, s2, 0);
					s.addWedstrijd(w, true);
		        	System.out.printf("Wedstrijd met spelers op plaats %d en %d \n", order[k], order[k+1] );
					wedstrijdnr++;
	        	}
	        	// trio
			    gws.addTrioWedstrijd(new Wedstrijd(wedstrijdnr, wedstrijdgroep.getSpelerByID(order[trioloc-1]+1), wedstrijdgroep.getSpelerByID(order[trioloc]+1), 0));
	        	System.out.printf("Wedstrijd uit trio met spelers op plaats %d en %d \n", order[trioloc-1], order[trioloc]);
			    wedstrijdnr++;
			    gws.addTrioWedstrijd(new Wedstrijd(wedstrijdnr, wedstrijdgroep.getSpelerByID(order[trioloc]+1), wedstrijdgroep.getSpelerByID(order[trioloc+1]+1), 0));
	        	System.out.printf("Wedstrijd uit trio met spelers op plaats %d en %d \n", order[trioloc], order[trioloc+1]);
			    wedstrijdnr++;
			    gws.addTrioWedstrijd(new Wedstrijd(wedstrijdnr, wedstrijdgroep.getSpelerByID(order[trioloc-1]+1), wedstrijdgroep.getSpelerByID(order[trioloc+1]+1), 0));
	        	System.out.printf("Wedstrijd uit trio met spelers op plaats %d en %d \n", order[trioloc-1], order[trioloc+1]);
			    wedstrijdnr++;
	        	// Einde trio
	        }
		//}
		if (s!= null) {
			s.renumber(); // Hernummer wedstrijden.
			gws.addSerie(s);
			logger.log(Level.INFO, "Voeg Serie toe");
			groep = updateSpelers(groep, s);
			logger.log(Level.INFO, "Update Spelers");
			// update gegevens tegenstanders en witvoorkeur
		}
		
		
//		boolean[] gepland = new boolean[groep.getSpelers().size()];
//		int aantalSpelers = groep.getSpelers().size();
//		ArrayList<Integer> trio = new ArrayList<>();
//		if (groep.getAantalSpelers() % 2 != 0) {
//	    	logger.log(Level.INFO, "Maken van een trio vanwege oneven aantal spelers");    		
//		    // Bij oneven aantal spelers wordt een trio gemaakt.
//		    trio = maakTrioWedstrijden(groep);
//		    aantalSpelers -= 3;
//		    Speler sid1 = groep.getSpelerByID(trio.get(0).intValue());
//		    Speler sid2 = groep.getSpelerByID(trio.get(1).intValue());
//		    Speler sid3 = groep.getSpelerByID(trio.get(2).intValue());
//	    	logger.log(Level.INFO, "Spelers in trio " + sid1.getInitialen() + " " + sid2.getInitialen() + " " + sid3.getInitialen());    		
//		    gws.addTrioWedstrijd(new Wedstrijd(997, sid1, sid2, 0));
//		    gws.addTrioWedstrijd(new Wedstrijd(998, sid2, sid3, 0));
//		    gws.addTrioWedstrijd(new Wedstrijd(999, sid1, sid3, 0));
//		}
//		for (int i = 0; i < speelrondes; ++i) {
//		    int minverschil = bepaalMinimaalVerschil(groep, periode, ronde, i + 1);
//		    for (int j = 0; j < gepland.length; ++j) {
//		        gepland[j] = false;
//		    }
//		    for (Integer sid : trio) {
//		        // -1 omdat speler ID één versprongen is tov array nummer
//		        gepland[sid.intValue() - 1] = true;
//		    }
//
//		    Serie serie = null;
//		    int ignoreTgns = 0;
//		    while ((serie == null) && (ignoreTgns <= 5)) {
//		        serie = maakSerie(groep, gepland, aantalSpelers, minverschil, ignoreTgns, ronde);
//		        ignoreTgns++;
//		    }
//
//			if (serie != null) {
//				gws.addSerie(serie);
//				groep = updateSpelers(groep, serie);
//				// update gegevens tegenstanders en witvoorkeur
//			}
//		}
		return gws;
	}

    public Serie maakSerie(Groep groep, boolean[] gepland, int aantalSpelers, int minverschil, int ignoreTgn, int ronde) {
        Serie serie = new Serie();
        int mv = minverschil;
        while (mv >= 0) {
            Serie s = planSerie(serie, groep.getSpelers(), gepland, aantalSpelers, minverschil, ignoreTgn, groep.getNiveau(), 1, ronde);
            if (s != null) {
                return s;
            }
            mv--;
        }
        return null;
    }

    private Serie planSerie(Serie serie, ArrayList<Speler> spelers, boolean[] gepland,
            int teplannen, int minverschil, int ignoreTgn, int niveau, int diepte, int ronde) {
        for (int i = 0; i < diepte; ++i) {
            System.out.print("  ");
        }
        System.out.print("vanaf:" + eersteOngeplandeSpeler(gepland, 0) + "#" + teplannen + "mv:" + minverschil);
        System.out.print(",itn:" + ignoreTgn + ",niv:" + niveau + "\n");
        
        // Laatste ronde?
        if (teplannen < 2) {
            return new Serie();
        }
        // Eerst doorgeschoven spelers inplannen
        // Maar deze speciale behandeling geldt alleen de eerste ronde
        int doorgeschovenID = laatsteOngeplandeDoorgeschovenspeler(spelers, gepland, niveau);
        if ((doorgeschovenID >= 0) && (ronde == 1)) {
            int zoekId = doorgeschovenID - 1;
            while (zoekId != -1) {
                int partner = laatsteOngeplandeSpeler(gepland, zoekId);
                if (partner == -1) {
                    return null;
                }
                Speler s1 = spelers.get(doorgeschovenID);
                Speler s2 = spelers.get(partner);
                if (!s1.isGespeeldTegen(s2, minverschil) && (s2.getGroep() != s1.getGroep())) {
                    gepland[doorgeschovenID] = true;
                    gepland[partner] = true;
                    Serie s = planSerie(serie, spelers, gepland, teplannen - 2, minverschil, ignoreTgn, niveau, diepte + 1, ronde);
                    if (s != null) {
                        Wedstrijd w = new Wedstrijd(diepte, s1, s2, 0);
                        s.addWedstrijd(w, true);
                        return s;
                    }
                    gepland[doorgeschovenID] = false;
                    gepland[partner] = false;
                }
                zoekId = partner - 1;
            }
        } else {
            // Inplannen 'gewone' speler
            int plannenID = eersteOngeplandeSpeler(gepland, 0);
            int zoekID = plannenID + 1;
            while (zoekID < gepland.length) {
                int partner = eersteOngeplandeSpeler(gepland, zoekID);
                if (partner == -1) {
                    return null;
                }
                Speler s1 = spelers.get(plannenID);
                Speler s2 = spelers.get(partner);
                if (!s1.isGespeeldTegen(s2, ignoreTgn) && (s2.getId() - s1.getId() >= minverschil)) {
                    gepland[plannenID] = true;
                    gepland[partner] = true;
                    Serie s = planSerie(serie, spelers, gepland, teplannen - 2, minverschil, ignoreTgn, niveau, diepte + 1, ronde);
                    if (s != null) {
                        Wedstrijd w = new Wedstrijd(s1.getId() * 100 + s2.getId(), s1, s2, 0);
                        s.addWedstrijd(w, true);
                        return s;
                    }
                    gepland[plannenID] = false;
                    gepland[partner] = false;
                }
                zoekID = partner + 1;
            }
        }
        return null;
    }

    /**
     Vind de eerste ongeplande speler
     @param gepland
     @param start
     @return 
     */
    public static int eersteOngeplandeSpeler(boolean[] gepland, int start) {
        if ((start < 0) || (start >= gepland.length)) {
            return -1;
        }
        for (int i = start; i < gepland.length; ++i) {
            if (!gepland[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     Vind de laatste ongeplande speler beginnende bij start en terugzoekende naar 0
     @param gepland
     @param start
     @return 
     */
    public static int laatsteOngeplandeSpeler(boolean[] gepland, int start) {
        if ((start < 0) || (start >= gepland.length)) {
            return -1;
        }
        for (int i = start; i >= 0; --i) {
            if (!gepland[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     Vind eerste ongeplande speler zoekende vanaf onderen
     @param spelers
     @param gepland
     @param niveau
     @return 
     */
    private int laatsteOngeplandeDoorgeschovenspeler(ArrayList<Speler> spelers, boolean[] gepland, int niveau) {
        for (int i = (gepland.length - 1); i > 0; --i) {
            if (!gepland[i] && spelers.get(i).getGroep() != niveau) {
                return i;
            }
        }
        return -1;
    }

    
    public int[][] MaakFuzzyMatrix(Groep wedstrijdgroep, int serie) {
    	/**
    	 *  FuzzyMatrix wordt gebruik voor het snel vaststellen van beste match als tegenstander door middel van Fuzzy Logic.
    	 *  Hiertoe worden per voorwaarde waaraan voldaan moet worden een matrix opgesteld.
    	 *  Per voorwaarde wordt bepaald hoe zwaar het weegt als niet aan de voorwaarde wordt voldaan.
    	 *  
    	 *  Voorwaarde 1: Niet tegen dezelfde tegenstander speler als in de laatste 4 partijen.
    	 *  Hiertoe wordt vastgesteld dat een tegenstander in één van de laatste twee ronden 70 weegt.
    	 *  Een tegenstander waar al tegen gestreden is in de twee-na-laatste of drie-na-laatste partij weegt 30.
    	 *  Een partij langer geleden is gewenst en weegt 0.
    	 *  
    	 *  Voorwaarde 2: Geen speler die een veel hogere of lagere ranking heeft.
    	 *  Hiertoe wordt vastgesteld dat een tegenstander 1 ranking  hoger/lager 0 weegt.
    	 *  Een tegenstander 2 rankings hoger/lager weegt 10.
    	 *  Een tegenstander 3 rankings hoger/lager weegt 20.
    	 *  Een tegenstander 4 rankings hoger/lager weegt 30.
    	 *  Een tegenstander 5 rankings hoger/lager weegt 50.
    	 *  Een tegenstander 6 rankings hoger/lager weegt 80.
    	 *  Een tegenstander 7 of meer rankings hoger/lager weegt 100.
    	 *  
    	 *  Voorwaarde 3: Iedere tegenstander moet zoveel mogelijk evenveel met wit als zwart spelen
    	 *  Hiertoe wordt een weging vastgesteld volgens het volgens matrix.
    	 *  
    	 *  			0		z1		z2		w1		w2
    	 *  			0		-50		-100	50		100
    	 *  0	0		20		35		50		35		50
    	 * z1	-50		35		60		75		10		25
    	 * z2	-100	50		75		100		25		0
    	 * w1	50		35		10		25		60		75
    	 * w2	100		50		25		0		75		100
    	 * 
    	 * Indien het om doorschuiven gaat en het om de eerste serie gaat is er nog een 4e voorwaarde.
    	 * De doorschuivende speler moet tegen iemand van de hogere groep zijn.
    	 * 
    	 * Een speler van de eigen groep weegt 100
    	 * Een speler van de hogere groep weegt 0
    	 * 
    	 * Indien het om doorschuiven gaat en het om de tweede serie gaat is er een andere 4e voorwaarde.
    	 * De dooschuivende speler speelt bij voorkeur tegen iemand van zijn eigen groep.
    	 * 
    	 * Een speler van de eigen groep weegt 0
    	 * Een speler van de hogere groep weegt 40
    	 * 
    	 * De vierkante matrices met dimensie (aantal spelers,aantal spelers) worden bij elkaar opgesteld.
    	 * Dit genereert een matrix met integers. Deze wordt hierna geoptimaliseerd door 
    	 * de diagonaal (is al nul) en de sub- en superdiagonaal te minimaliseren. Hiertoe wordt een iteratie uitgevoerd van 
    	 * algorithme minimizetrigonal 
 
    	 */
        int matrix1[][]  = new int[wedstrijdgroep.getAantalSpelers()][wedstrijdgroep.getAantalSpelers()];
        int matrix2[][]  = new int[wedstrijdgroep.getAantalSpelers()][wedstrijdgroep.getAantalSpelers()];
        int matrix3[][]  = new int[wedstrijdgroep.getAantalSpelers()][wedstrijdgroep.getAantalSpelers()];
        int matrix4[][]  = new int[wedstrijdgroep.getAantalSpelers()][wedstrijdgroep.getAantalSpelers()];
        int matrix[][]  = new int[wedstrijdgroep.getAantalSpelers()][wedstrijdgroep.getAantalSpelers()];
        int i,j,weging=0;
        int tegenstanders[] = new int [4];
        // matrix1
        System.out.print("Initializing Matrix1\n");
        for (i=1; i<=wedstrijdgroep.getAantalSpelers();i++){
        	for (j=1;j<=wedstrijdgroep.getAantalSpelers();j++){
        		//logger.log(Level.INFO, "Speler 1 ID : " + wedstrijdgroep.getSpelerByID(i));
        		//logger.log(Level.INFO, "Speler 2 ID : " + wedstrijdgroep.getSpelerByID(j));
            	weging=0;
        		tegenstanders = wedstrijdgroep.getSpelerByID(i).getGespeeldTegen(wedstrijdgroep.getSpelerByID(j));
        		for (int k = 0; k <4; k++){
            		if ((tegenstanders[k] >0) && (tegenstanders[k] <3)) {
            			weging+=70;
                		System.out.print(tegenstanders[k] + " ronden eerder al gespeeld tegen " + wedstrijdgroep.getSpelerByID(j).getNaam() +"\n");
            		}
            		if ((tegenstanders[k] >2) && (tegenstanders[k] <5)) {
            			weging+=30;
                		System.out.print(tegenstanders[k] + " ronden eerder al gespeeld tegen " + wedstrijdgroep.getSpelerByID(1).getNaam() +"\n");
            		}
        			
        		}
            	matrix1[i-1][j-1]=weging;
        	}
        }
    	Utils.printMatrix(matrix1);
        // matrix2
        System.out.print("Initializing Matrix2\n");
        for (i=1; i<=wedstrijdgroep.getAantalSpelers();i++){
        	for (j=1;j<=wedstrijdgroep.getAantalSpelers();j++){
        		switch (Math.abs(j-i)) {
        		case 0:
        		case 1:
        			matrix2[i-1][j-1]=0;
        			break;
        		case 2:
        			matrix2[i-1][j-1]=10;
        			break;
        		case 3:
        			matrix2[i-1][j-1]=20;
        			break;
        		case 4:
        			matrix2[i-1][j-1]=30;
        			break;
        		case 5:
        			matrix2[i-1][j-1]=50;
        			break;
        		case 6:
        			matrix2[i-1][j-1]=80;
        			break;
        		default:
        			matrix2[i-1][j-1]=100;
        			break;
        		}
        	}
    	}
    	Utils.printMatrix(matrix2);
    	// matrix 3
    	System.out.print("Initializing Matrix3\n");
        for (i=1; i<=wedstrijdgroep.getAantalSpelers();i++){
        	int witv1 = (int) wedstrijdgroep.getSpelerByID(i).getWitvoorkeur();
        	for (j=1;j<=wedstrijdgroep.getAantalSpelers();j++){
        		int witv2 = (int) wedstrijdgroep.getSpelerByID(j).getWitvoorkeur();
        		if (i==j) {
        			matrix3[i-1][j-1] = 0;
        		} else {
        			switch (witv1) {
        			case -2:
        				switch (witv2) {
        				case -2:
        					matrix3[i-1][j-1]=100;
        					break;
        				case -1:
        					matrix3[i-1][j-1]=75;
        					break;
        				case 0:
        					matrix3[i-1][j-1]=50;
        					break;
        				case 1:
        					matrix3[i-1][j-1]=25;
        					break;
        				case 2:
        					matrix3[i-1][j-1]=0;
        					break;
        				}
        				break;
        			case -1:
        				switch (witv2) {
        				case -2:
        					matrix3[i-1][j-1]=75;
        					break;
        				case -1:
        					matrix3[i-1][j-1]=60;
        					break;
        				case 0:
        					matrix3[i-1][j-1]=35;
        					break;
        				case 1:
        					matrix3[i-1][j-1]=10;
        					break;
        				case 2:
        					matrix3[i-1][j-1]=25;
        					break;
        				}        			
        				break;
        			case 0:
        				switch (witv2) {
        				case -2:
        					matrix3[i-1][j-1]=50;
        					break;
        				case -1:
        					matrix3[i-1][j-1]=25;
        					break;
        				case 0:
        					matrix3[i-1][j-1]=20;
        					break;
        				case 1:
        					matrix3[i-1][j-1]=35;        				
        					break;
        				case 2:
        					matrix3[i-1][j-1]=50;
        					break;
        				}
        				break;
        			case 1:
        				switch (witv2) {
        				case -2:
        					matrix3[i-1][j-1]=25;
        					break;
        				case -1:
        					matrix3[i-1][j-1]=10;
        					break;
        				case 0:
        					matrix3[i-1][j-1]=35;
        					break;
        				case 1:
        					matrix3[i-1][j-1]=60;
        					break;
        				case 2:
        					matrix3[i-1][j-1]=75;
        					break;
        				}
        				break;
        			case 2:
        				switch (witv2) {
        				case -2:
        					matrix3[i-1][j-1]=0;
        					break;
        				case -1:
        					matrix3[i-1][j-1]=25;
        					break;
        				case 0:
        					matrix3[i-1][j-1]=50;
        					break;
        				case 1:
        					matrix3[i-1][j-1]=75;
        					break;
        				case 2:
        					matrix3[i-1][j-1]=100;
        					break;
        				}
        				break;
        			};
        		}
        	}
        }
        Utils.printMatrix(matrix3);
    	// matrix 4
        System.out.print("Initializing Matrix4\n");
        for (i=1; i<=wedstrijdgroep.getAantalSpelers();i++){
        	for (j=1;j<=wedstrijdgroep.getAantalSpelers();j++){
        		if (i == j) {
        			matrix4[i-1][j-1] = 0;
        		} else{
        			switch (serie) {
        			case 1:
        				if (wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
        					matrix4[i-1][j-1] = 100;
        				} else{
            				if (wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && !(wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j)))) {
                				matrix4[i-1][j-1] = 0;
            				}
        				}
        				if (!wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
            				matrix4[i-1][j-1] = 0;
        				} else{
            				if (!wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && !wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
                				matrix4[i-1][j-1] = 20;
            				}
        				}
        				break;
        			case 2:
        				if (wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
            				matrix4[i-1][j-1] = 0;
        				} else{
            				if (wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
            					matrix4[i-1][j-1] = 40;
            				}
        				}
        				if (!wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
            				matrix4[i-1][j-1] = 40;
        				} else{
            				if (!wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(i)) && !wedstrijdgroep.getSpelersMetAnderNiveau().contains(wedstrijdgroep.getSpelerByID(j))) {
                				matrix4[i-1][j-1] = 20;
            				}
        				}
        				break;
        			}
        		}
        	}
        }
        Utils.printMatrix(matrix4);
        matrix=Utils.add2DArrays(matrix1,matrix2);
        matrix=Utils.add2DArrays(matrix,matrix3);
        matrix=Utils.add2DArrays(matrix,matrix4);
        System.out.print("Output Matrix\n");
        Utils.printMatrix(matrix);
        return matrix;
    }


    /**
     * Update de volgende gegevens van een speler: - Witvoorkeur - Tegenstanders
     *
     * @param groep
     * @param serie
     * @return
     */
    public Groep updateSpelers(Groep groep, Serie serie) {

        for (Speler speler : groep.getSpelers()) {
            Wedstrijd wedstrijd = serie.getWedstrijdVoorSpeler(speler);
            if (wedstrijd != null) {
                if (wedstrijd.getWit() == speler) {
                    // Speler speelde met wit
                    speler.addTegenstander(wedstrijd.getZwart().getInitialen());
                    speler.setWitvoorkeur(speler.getWitvoorkeur() - 1.1);
                } else if (wedstrijd.getZwart() == speler) {
                    // Speler speelde met zwart
                    speler.addTegenstander(wedstrijd.getWit().getInitialen());
                    speler.setWitvoorkeur(speler.getWitvoorkeur() + 1.1);
                } else {
                    System.out.println("Hmmm, speler niet gevonden....");
                }
            }
        }
        return groep;
    }

    /**
     * Maak trio wedstrijden voor betreffende groep
     * @param groep Groep
     * @return
     */
    private ArrayList<Integer> maakTrioWedstrijden(Groep groep) {
        ArrayList<Integer> trio = new ArrayList<>();
        if (groep.getSpelers().size() == 3) {
        	// 3 spelers, dus maak gelijk trio
            trio.add(groep.getSpelers().get(0).getId());
            trio.add(groep.getSpelers().get(1).getId());
            trio.add(groep.getSpelers().get(2).getId());
            return trio;
        }
        int spelerID = groep.getSpelers().size() / 2;
        int minDelta = 1;
        int plusDelta = 1;
        int ignore = 0;
        boolean doorzoeken = true;
        while (doorzoeken) {
            Speler s1 = groep.getSpelerByID(spelerID);
            Speler s2 = groep.getSpelerByID(spelerID - minDelta);
            Speler s3 = groep.getSpelerByID(spelerID + plusDelta);
            if (isGoedTrio(s1, s2, s3, ignore)) {
                trio.add(s1.getId());
                trio.add(s2.getId());
                trio.add(s3.getId());
                return trio;
            } else {
                if ((s2 == null) || (s3 == null)) {
                    if (ignore > 4) {
                        doorzoeken = false;
                    }
                    ignore += 1;
                    minDelta = 1;
                    plusDelta = 1;
                } else {
                    if (minDelta > plusDelta) {
                        plusDelta++;
                    } else {
                        minDelta++;
                    }
                }
            }
        }
        return trio;
    }

    /**
     * Stel vast op het meegegeven trio een goed trio is conform
     * de regels. 
     * @param s1 Speler 1
     * @param s2 Speler 2 
     * @param s3 Speler 3
     * @param ignore Aantal te negeren rondes in het verleden 
     * @return
     */
    private boolean isGoedTrio(Speler s1, Speler s2, Speler s3, int ignore) {
        if ((s1 != null) && (s2 != null) && (s3 != null)) {
            return !s1.isGespeeldTegen(s2, ignore) && !s1.isGespeeldTegen(s3, ignore) && !s2.isGespeeldTegen(s1, ignore)
                    && !s2.isGespeeldTegen(s3, ignore) && !s3.isGespeeldTegen(s1, ignore)
                    && !s3.isGespeeldTegen(s2, ignore);
        } else {
            return false;
        }
    }
}
