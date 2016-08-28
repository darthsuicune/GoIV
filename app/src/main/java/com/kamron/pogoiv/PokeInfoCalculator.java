package com.kamron.pogoiv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Johan Swanberg on 2016-08-18.
 * A class which interprets pokemon information
 */
public class PokeInfoCalculator {

    public ArrayList<Pokemon> pokedex = null;
    public HashMap<String, Pokemon> pokemap = null;

    /**
     * creates a pokemon info calculator with the pokemon as argument
     *
     * @param namesArray      array of all pokemon names
     * @param attackArray     array of all pokemon base attack stat
     * @param defenceArray    array of all pokemon base def stat
     * @param staminaArray    array of all pokemon base stam stat
     * @param devolutionArray array of what the pokemon evolved from, -1 if no devolution
     */
    public PokeInfoCalculator(String[] namesArray, int[] attackArray, int[] defenceArray, int[] staminaArray, int[] devolutionArray) {
        populatePokemon(namesArray, attackArray, defenceArray, staminaArray, devolutionArray);
    }

    public Pokemon get(int number) {
        return pokedex.get(number);
    }

    public Pokemon get(String name) {
        return pokemap.get(name);
    }

    /**
     * Fills the list "pokemon" with the information of all pokemon by reading the
     * arrays in integers.xml and the names from the strings.xml resources.
     */
    private void populatePokemon(String[] names, int[] attack, int[] defense, int[] stamina, int[] devolution) {
        pokedex = new ArrayList<>();
        pokemap = new HashMap<>();

        int pokeListSize = names.length;
        for (int i = 0; i <= pokeListSize - 1; i++) {
            Pokemon p = new Pokemon(names[i], i, attack[i], defense[i], stamina[i], devolution[i]);
            pokedex.add(p);
            pokemap.put(names[i], p);
        }

        for (int i = 0; i <= pokeListSize - 1; i++) {
            if (devolution[i] != -1) {
                Pokemon devo = pokedex.get(devolution[i]);
                devo.evolutions.add(pokedex.get(i));
                sortPokedex(devo.evolutions);
            }
        }

        // don't sort or otherwise we loose evolution-order of the list
        //sortPokedex(pokedex);
    }


    /**
     * Sorts the pokemon in the pokedex by alphabetical order
     *
     * @param pokedex
     */
    private void sortPokedex(List<Pokemon> pokedex) {
        //Sort pokemon alphabetically (maybe just do this in the res files?)
        Collections.sort(pokedex, new Comparator<Pokemon>() {
                    public int compare(Pokemon lhs, Pokemon rhs) {
                        return lhs.name.compareTo(rhs.name);
                    }
                }
        );
    }


    /**
     * getUpgradeCost
     * Gets the needed required candy and stardust to hit max level (relative to trainer level)
     *
     * @param goalLevel             The level to reach
     * @param estimatedPokemonLevel The estimated level of hte pokemon
     * @return The text that shows the amount of candy and stardust needed.
     */
    public UpgradeCost getUpgradeCost(float goalLevel, double estimatedPokemonLevel) {
        int neededCandy = 0;
        int neededStarDust = 0;
        while (estimatedPokemonLevel != goalLevel) {
            int rank = 5;
            if ((estimatedPokemonLevel % 10) >= 1 && (estimatedPokemonLevel % 10) <= 2.5)
                rank = 1;
            else if ((estimatedPokemonLevel % 10) > 2.5 && (estimatedPokemonLevel % 10) <= 4.5)
                rank = 2;
            else if ((estimatedPokemonLevel % 10) > 4.5 && (estimatedPokemonLevel % 10) <= 6.5)
                rank = 3;
            else if ((estimatedPokemonLevel % 10) > 6.5 && (estimatedPokemonLevel % 10) <= 8.5)
                rank = 4;

            if (estimatedPokemonLevel <= 10.5) {
                neededCandy++;
                neededStarDust += rank * 200;
            } else if (estimatedPokemonLevel > 10.5 && estimatedPokemonLevel <= 20.5) {
                neededCandy += 2;
                neededStarDust += 1000 + (rank * 300);
            } else if (estimatedPokemonLevel > 20.5 && estimatedPokemonLevel <= 30.5) {
                neededCandy += 3;
                neededStarDust += 2500 + (rank * 500);
            } else if (estimatedPokemonLevel > 30.5) {
                neededCandy += 4;
                neededStarDust += 5000 + (rank * 1000);
            }

            estimatedPokemonLevel += 0.5;
        }
        return new UpgradeCost(neededStarDust, neededCandy);
    }


    /**
     * Adds rows with IV information (up to 8) in hte returnVal input string, and gives an IVscanResult object
     * with information about the pokemon
     *
     * @param estimatedPokemonLevel The estimated pokemon level
     * @param pokemonHP             THe pokemon hp
     * @param pokemonCP             The pokemonCP
     * @return An IVScanResult which contains the information calculated about the pokemon, or null if there are too many possibilities.
     */
    public IVScanResult getIVPossibilities(int selectedPokemon, double estimatedPokemonLevel, int pokemonHP, int pokemonCP) {
        int baseAttack = get(selectedPokemon).baseAttack;
        int baseDefense = get(selectedPokemon).baseDefense;
        int baseStamina = get(selectedPokemon).baseStamina;

        double lvlScalar = Data.CpM[(int) (estimatedPokemonLevel * 2 - 2)];
        double lvlScalarPow2 = Math.pow(lvlScalar, 2) * 0.1; // instead of computing again in every loop
        //for averagePercent
        int sumIV;
        int averageSum = 0;
        //IV vars for lower and upper end cp ranges


        //It's safe to proceed if *one* is not 10, though it takes a bit longer.
        if (pokemonHP != 10 || pokemonCP != 10) {
            IVScanResult returner = new IVScanResult(get(selectedPokemon), estimatedPokemonLevel);
            for (int staminaIV = 0; staminaIV < 16; staminaIV++) {
                int hp = (int) Math.max(Math.floor((baseStamina + staminaIV) * lvlScalar), 10);
                if (hp == pokemonHP) {
                    double lvlScalarStamina = Math.sqrt(baseStamina + staminaIV) * lvlScalarPow2;
                    //Possible STA IV
                    //System.out.println("Checking sta: " + staminaIV + ", gives " + hp);
                    for (int defenseIV = 0; defenseIV < 16; defenseIV++) {
                        for (int attackIV = 0; attackIV < 16; attackIV++) {
                            int cp = (int) Math.floor((baseAttack + attackIV) * Math.sqrt(baseDefense + defenseIV) * lvlScalarStamina);
                            if (cp == pokemonCP) {
                                returner.addIVCombination(attackIV, defenseIV, staminaIV);
                            }
                        }
                    }
                } else if (hp > pokemonHP) {
                    break;
                }
            }
            return returner;
        } else {
            return new IVScanResult(get(selectedPokemon), estimatedPokemonLevel, true);

        }
    }


    /**
     * getCpAtRangeLeve
     * Used to calculate CP ranges for a species at a specific level based on the lowest and highest
     * IV combination.
     * <p/>
     * Returns a string on the form of "\n CP at lvl X: A - B" where x is the pokemon level, A is minCP and B is maxCP
     *
     * @param pokemon     the index of the pokemon species within the pokemon list (sorted)
     * @param lowAttack   attack IV of the lowest combination
     * @param lowDefense  defense IV of the lowest combination
     * @param lowStamina  stamina IV of the lowest combination
     * @param highAttack  attack IV of the highest combination
     * @param highDefense defense IV of the highest combination
     * @param highStamina stamina IV of the highest combination
     * @param level       pokemon level for CP calculation
     * @return String containing the CP range including the specified level.
     */
    public CPRange getCpRangeAtLevel(Pokemon pokemon, int lowAttack, int lowDefense, int lowStamina, int highAttack, int highDefense, int highStamina, double level) {
        int baseAttack = pokemon.baseAttack;
        int baseDefense = pokemon.baseDefense;
        int baseStamina = pokemon.baseStamina;
        double lvlScalar = Data.CpM[(int) (level * 2 - 2)];
        int cpMin = (int) Math.floor((baseAttack + lowAttack) * Math.sqrt(baseDefense + lowDefense) * Math.sqrt(baseStamina + lowStamina) * Math.pow(lvlScalar, 2) * 0.1);
        int cpMax = (int) Math.floor((baseAttack + highAttack) * Math.sqrt(baseDefense + highDefense) * Math.sqrt(baseStamina + highStamina) * Math.pow(lvlScalar, 2) * 0.1);
        if (cpMin > cpMax) {
            int tmp = cpMax;
            cpMax = cpMin;
            cpMin = tmp;
        }
        return new CPRange(cpMax, cpMin, level);
    }

    /**
     * get the lowest evolution in the chain of a pokemon
     *
     * @param poke a pokemon, example charizard
     * @return a pokemon, in the example would return charmander
     */
    public Pokemon getLowestEvolution(Pokemon poke) {
        if (poke.devoNumber < 0) return poke; //already lowest evolution

        Pokemon devoPoke = get(poke.devoNumber);
        while (devoPoke.devoNumber >= 0) { //while devol
            devoPoke = get(devoPoke.devoNumber);
        }
        return devoPoke;
    }

    /**
     * returns the higher evolutions of a pokemon plus itself
     *
     * @param poke the pokemon to return itself and higher evolutions of itself
     * @return a list with pokemon, input pokemon plus its evolutions
     */
    public ArrayList<Pokemon> getEvolutionLine(Pokemon poke) {
        poke = getLowestEvolution(poke);

        ArrayList list = new ArrayList();
        list.add(poke); //add self
        list.addAll(poke.evolutions); //add all immediate evolutions
        for (Pokemon evolution : poke.evolutions) {
            list.addAll(evolution.evolutions);
        }

        return list;
    }

}
