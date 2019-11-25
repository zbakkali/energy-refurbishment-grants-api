package com.jad.energy.refurbishment.grants.api.model;

import java.util.List;

public class ScenarioSubvention {

    private final Scenario scenario;
    private final double quotePartRenovation;
    private final double quotePartRenovationAvecSubvention;
    private final List<Subvention> subventions;

    public ScenarioSubvention(Scenario scenario, double quotePartRenovation, double quotePartRenovationAvecSubvention, List<Subvention> subventions) {
        this.scenario = scenario;
        this.quotePartRenovation = quotePartRenovation;
        this.quotePartRenovationAvecSubvention = quotePartRenovationAvecSubvention;
        this.subventions = subventions;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public double getQuotePartRenovation() {
        return quotePartRenovation;
    }

    public double getQuotePartRenovationAvecSubvention() {
        return quotePartRenovationAvecSubvention;
    }

    public List<Subvention> getSubventions() {
        return subventions;
    }
}
