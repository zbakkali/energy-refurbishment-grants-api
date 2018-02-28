package com.jad.energy.refurbishment.grants.api.model;

import java.util.List;

public class ScenarioSubvention {

    private final double scenario;
    private final double partTravaux;
    private final double cout;
    private final List<Subvention> subventions;

    public ScenarioSubvention(final double scenario, final double partTravaux, final double cout, final List<Subvention> subventions) {
        this.scenario = scenario;
        this.partTravaux = partTravaux;
        this.cout = cout;
        this.subventions = subventions;
    }

    public double getScenario() {
        return scenario;
    }

    public double getPartTravaux() {
        return partTravaux;
    }

    public double getCout() {
        return cout;
    }

    public List<Subvention> getSubventions() {
        return subventions;
    }
}
