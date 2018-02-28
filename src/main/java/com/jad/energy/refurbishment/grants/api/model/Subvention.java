package com.jad.energy.refurbishment.grants.api.model;

import java.util.Set;

public class Subvention {
    private final String name;
    private final double montant;
    private final Set<String> conditions;
    private final Set<String> infos;

    public Subvention(final String name,
                      final double montant,
                      final Set<String> conditions,
                      final Set<String> infos) {

        this.name = name;
        this.montant = montant;
        this.conditions = conditions;
        this.infos = infos;
    }

    public String getName() {
        return name;
    }

    public double getMontant() {
        return montant;
    }

    public Set<String> getConditions() {
        return conditions;
    }

    public Set<String> getInfos() {
        return infos;
    }
}
