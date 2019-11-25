package com.jad.energy.refurbishment.grants.api.model;

public class Scenario {

    private final String name;
    private final String description;
    private final double gainEnergetique;
    private final double coutCoproprieteTravaux;
    private final double coutCoproprieteMOE;
    private final double coutCoproprieteAMO;
    private final double coutCoproprieteAssurance;
    private final double coutCoproprieteDiagnostiqueAmiante;
    private final double coutCoproprieteRenovation;

    public Scenario(String name, String description, double gainEnergetique, double coutCoproprieteTravaux, double coutCoproprieteMOE, double coutCoproprieteAMO, double coutCoproprieteAssurance, double coutCoproprieteDiagnostiqueAmiante) {
        this.name = name;
        this.description = description;
        this.gainEnergetique = gainEnergetique;
        this.coutCoproprieteTravaux = coutCoproprieteTravaux;
        this.coutCoproprieteMOE = coutCoproprieteMOE;
        this.coutCoproprieteAMO = coutCoproprieteAMO;
        this.coutCoproprieteAssurance = coutCoproprieteAssurance;
        this.coutCoproprieteDiagnostiqueAmiante = coutCoproprieteDiagnostiqueAmiante;
        this.coutCoproprieteRenovation = coutCoproprieteTravaux + coutCoproprieteMOE + coutCoproprieteAMO + coutCoproprieteAssurance + coutCoproprieteDiagnostiqueAmiante;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getGainEnergetique() {
        return gainEnergetique;
    }

    public double getCoutCoproprieteTravaux() {
        return coutCoproprieteTravaux;
    }

    public double getCoutCoproprieteMOE() {
        return coutCoproprieteMOE;
    }

    public double getCoutCoproprieteAMO() {
        return coutCoproprieteAMO;
    }

    public double getCoutCoproprieteAssurance() {
        return coutCoproprieteAssurance;
    }

    public double getCoutCoproprieteDiagnostiqueAmiante() {
        return coutCoproprieteDiagnostiqueAmiante;
    }

    public double getCoutCoproprieteRenovation() {
        return coutCoproprieteRenovation;
    }
}
