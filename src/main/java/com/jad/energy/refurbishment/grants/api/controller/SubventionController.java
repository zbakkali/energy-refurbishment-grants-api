package com.jad.energy.refurbishment.grants.api.controller;

import com.jad.energy.refurbishment.grants.api.model.RequesterTypeEnum;
import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import com.jad.energy.refurbishment.grants.api.model.ScenarioSubvention;
import com.jad.energy.refurbishment.grants.api.model.Subvention;
import com.jad.energy.refurbishment.grants.api.service.RevenusTypeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@RefreshScope
@RestController
public class SubventionController {

    @Value("${message:Hello default}")
    private String message;

    @GetMapping("/message")
    String getMessage() {
        return this.message;
    }

    private RevenusTypeService anahRevenusTypeServiceImpl;
    private RevenusTypeService departementalRevenusTypeServiceImpl;

    public SubventionController(RevenusTypeService anahRevenusTypeServiceImpl,
                                RevenusTypeService departementalRevenusTypeServiceImpl) {

        this.anahRevenusTypeServiceImpl = anahRevenusTypeServiceImpl;
        this.departementalRevenusTypeServiceImpl = departementalRevenusTypeServiceImpl;
    }

    @GetMapping("compute_anah_revenus_type")
    public String computeAnahRevenusType(@RequestParam final double revenus,
                                         @RequestParam final int composition,
                                         @RequestParam final RequesterTypeEnum requesterType) {

        // plafonds
        final RevenusTypeEnum anahRevenusType = anahRevenusTypeServiceImpl.compute(revenus, composition);

        return anahRevenusType.name();
    }

    @CrossOrigin
    @GetMapping("compute_subvention")
    public List<ScenarioSubvention> computeSubvention(@RequestParam final double revenus,
                                                      @RequestParam final long quotepart1,
                                                      @RequestParam final long quotepart2,
                                                      @RequestParam final int composition,
                                                      @RequestParam final RequesterTypeEnum requesterType,
                                                      @RequestParam(required = false) final Double surface) {

        final Map<Double, Double> scenariosTravauxTTC = Map.of(
                5384683D, 137293D,
                6131351D, 91447D,
                6440118D, 131925D
        );

        final Double tva = 5.5 / 100;
        final int habitationCount = 398;

        //final Double quotePart = 344D / 200000;
        final Double quotePart = 1D * quotepart1 / quotepart2;

        switch (requesterType) {
            case PROPRIETAIRE_OCCUPANT: {
                final RevenusTypeEnum anahRevenusType = anahRevenusTypeServiceImpl.compute(revenus, composition);
                final RevenusTypeEnum departementalRevenusType = departementalRevenusTypeServiceImpl.compute(revenus, composition);
                return scenariosTravauxTTC.entrySet().stream()
                        .map(travauxTotal -> {
                            final double travauxPartTTC = Math.round(travauxTotal.getKey() * quotePart);
                            final double travauxPartHT = travauxPartTTC * (1 - tva);

                            final Subvention anahSubvention = getProprietaireOccupantAnahSubvention(anahRevenusType, travauxPartHT);
                            final Subvention habiterMieuxSubvention = getProprietaireOccupantHabiterMieuxSubvention(anahRevenusType, travauxPartHT);
                            final Subvention ceeSubvention = new Subvention("cee", Math.round(travauxTotal.getValue() * quotePart), null, null);
                            final Subvention habiterMieuxCoproSubvention = getProprietaireOccupantHabiterMieuxCoproSubvention(travauxTotal.getKey() * (1 - tva), habitationCount);

                            final double fixedSubvention = anahSubvention.getMontant() + habiterMieuxSubvention.getMontant() + ceeSubvention.getMontant() + habiterMieuxCoproSubvention.getMontant();

                            Subvention departementaleSubvention = getProprietaireOccupantDepartementalSubvention(departementalRevenusType, travauxPartHT);

                            double reste = travauxPartTTC - fixedSubvention - departementaleSubvention.getMontant();
                            Subvention citeSubvention = getProprietaireOccupantCITESubvention(reste, composition);
                            double cout = Math.round(reste - citeSubvention.getMontant());

                            double tauxSubvention = (travauxPartTTC - cout) / travauxPartTTC;
                            boolean departementaleSubventionRecompute = isDepartementaleSubventionRecompute(anahRevenusType, tauxSubvention);

                            if (departementaleSubventionRecompute) {
                                double resteWithoutCite;
                                switch (anahRevenusType) {
                                    case TRES_MODESTE:
                                        resteWithoutCite = getProprietaireOccupantWhitoutCITE(composition, 0);
                                        break;
                                    case MODESTE:
                                        resteWithoutCite = getProprietaireOccupantWhitoutCITE(composition, 20 / 100 * travauxPartTTC);
                                        break;
                                    case INTERMEDIAIRE:
                                        resteWithoutCite = getProprietaireOccupantWhitoutCITE(composition, 20 / 100 * travauxPartTTC);
                                        break;
                                    default:
                                        resteWithoutCite = reste;
                                }
                                double departementaleSubventionMontant = travauxPartTTC - fixedSubvention - resteWithoutCite;
                                if (departementaleSubventionMontant < 1000) {
                                    departementaleSubventionMontant = 0;
                                }
                                departementaleSubvention = getProprietaireOccupantDepartementalSubvention(departementalRevenusType, travauxPartHT, departementaleSubventionMontant);
                                reste = travauxPartTTC - fixedSubvention - departementaleSubvention.getMontant();
                                citeSubvention = getProprietaireOccupantCITESubvention(reste, composition);
                                cout = Math.round(reste - citeSubvention.getMontant());
                            }

                            return new ScenarioSubvention(
                                    travauxTotal.getKey(),
                                    travauxPartTTC,
                                    cout,
                                    List.of(anahSubvention, habiterMieuxSubvention, ceeSubvention, departementaleSubvention, habiterMieuxCoproSubvention, citeSubvention));
                        })
                        .collect(toList());
            }
            case PROPRIETAIRE_BAILLEUR: {
                return scenariosTravauxTTC.entrySet().stream()
                        .map(travauxTotal -> {
                            final double travauxPartTTC = Math.round(travauxTotal.getKey() * quotePart);
                            final double travauxPartHT = travauxPartTTC * (1 - tva);

                            final Subvention anahSubvention = getProprietaireBailleurAnahSubvention(travauxPartHT, surface);
                            final Subvention habiterMieuxSubvention = getProprietaireBailleurHabiterMieuxSubvention();
                            final Subvention ceeSubvention = new Subvention("cee", Math.round(travauxTotal.getValue() * quotePart), null, null);
                            final Subvention habiterMieuxCoproSubvention = getProprietaireOccupantHabiterMieuxCoproSubvention(travauxTotal.getKey() * (1 - tva), habitationCount);

                            final double fixedSubvention = anahSubvention.getMontant() + habiterMieuxSubvention.getMontant() + ceeSubvention.getMontant() + habiterMieuxCoproSubvention.getMontant();

                            double cout = travauxPartTTC - fixedSubvention;

                            return new ScenarioSubvention(
                                    travauxTotal.getKey(),
                                    travauxPartTTC,
                                    cout,
                                    List.of(
                                            anahSubvention,
                                            habiterMieuxSubvention,
                                            ceeSubvention,
                                            new Subvention("aide départementale à l'amélioration de l'habitat privé", 0, null, null),
                                            habiterMieuxCoproSubvention,
                                            new Subvention("CITE", 0, null, null)
                                    ));
                        })
                        .collect(toList());
            }
        }
        return null;
    }

    private boolean isDepartementaleSubventionRecompute(RevenusTypeEnum anahRevenusType, double tauxSubvention) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return tauxSubvention >= 1;
            case MODESTE:
                return tauxSubvention >= 80 / 100;
            case INTERMEDIAIRE:
                return tauxSubvention >= 80 / 100;
            default:
                return false;
        }
    }

    private Subvention getProprietaireOccupantHabiterMieuxCoproSubvention(double travauxHT, int habitationCount) {
        final double aide = Math.round(Math.min(travauxHT * 25 / 100 / habitationCount, 5250));
        return new Subvention("Habiter Mieux Copro", aide, null, null);
    }

    private Subvention getProprietaireOccupantAnahSubvention(final RevenusTypeEnum anahRevenusType, final double travauxPartHT) {
        final double aide = getProprietaireOccupantAnah(anahRevenusType, travauxPartHT);
        final Set<String> conditions = Set.of(
                "vous n'avez pas bénéficié d'un PTZ (Prêt à taux zéro pour l'accession à la propriété) depuis 5 ans",
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l’Anah l’enregistrement des Certificats d’Economie d’Energie (CEE) générés par les travaux de rénovation thermique",
                "vous engager à occuper le logement à titre de résidence principale pendant une durée minimale de 6 (six) ans après la fin des travaux."
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );

        return new Subvention("Aide de l'Anah : travaux d'amélioration de l'habitat", aide, conditions, infos);
    }

    private Subvention getProprietaireOccupantHabiterMieuxSubvention(final RevenusTypeEnum anahRevenusType, final double travauxPartHT) {
        final double aide = getProprietaireOccupantHabiterMieux(anahRevenusType, travauxPartHT);
        final Set<String> conditions = Set.of(
                "vous n'avez pas bénéficié d'un PTZ (Prêt à taux zéro pour l'accession à la propriété) depuis 5 ans",
                /*"vos travaux doivent permettre de faire baisser votre consommation énergétique d'au moins 25 %",*/
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l’Anah l’enregistrement des Certificats d’Economie d’Energie (CEE) générés par les travaux de rénovation thermique",
                "vous engager à occuper le logement à titre de résidence principale pendant une durée minimale de 6 (six) ans après la fin des travaux."
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );


        return new Subvention("prime Habiter Mieux", aide, conditions, infos);
    }

    private Subvention getProprietaireOccupantDepartementalSubvention(final RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        return getProprietaireOccupantDepartementalSubvention(anahRevenusType, travauxPartHT, null);
    }


    private Subvention getProprietaireOccupantDepartementalSubvention(final RevenusTypeEnum anahRevenusType, double travauxPartHT, Double departementaleSubventionMontant) {
        final double aide = departementaleSubventionMontant != null ? departementaleSubventionMontant : getProprietaireOccupantDepartemental(anahRevenusType, travauxPartHT);
        final Set<String> conditions = Set.of(
                "vous n'avez pas bénéficié d'un PTZ (Prêt à taux zéro pour l'accession à la propriété) depuis 5 ans",
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l’Anah l’enregistrement des Certificats d’Economie d’Energie (CEE) générés par les travaux de rénovation thermique",
                "vous engager à occuper le logement à titre de résidence principale pendant une durée minimale de 6 (six) ans après la fin des travaux."
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );

        return new Subvention("aide départementale à l'amélioration de l'habitat privé", aide, conditions, infos);

    }


    private Subvention getProprietaireBailleurAnahSubvention(final double travauxPartHT, final double surface) {

        final double aide = getProprietaireBailleurAnah(travauxPartHT, surface);
        final Set<String> conditions = Set.of(
                /*"Les travaux doivent permettent d'atteindre au moins l'étiquette énergétique D",*/
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "mettre en location ou continuer à louer votre bien décent et non meublé pour une durée minimum de 9 ans",
                "ne pas dépasser le montant de loyer maximal fixé localement par l'Anah",
                "louer, en tant que résidence principale, à des personnes dont les ressources sont inférieures aux plafonds fixés nationalement",
                "ne pas louer à des personnes de votre famille proche",
                "remettre le bien en location en cas de départ du locataire pendant la période couverte par la convention"
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );

        return new Subvention("Aide de l'Anah : travaux d'amélioration de l'habitat", aide, conditions, infos);
    }


    private Subvention getProprietaireBailleurHabiterMieuxSubvention() {
        final double aide = getProprietaireBailleurHabiterMieux();
        final Set<String> conditions = Set.of(
                "Les travaux doivent permettent d'atteindre au moins l'étiquette énergétique D",
                /*"les travaux de rénovation thermique doivent permettre un gain de 35 % de performances énergétiques.",*/
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l'Anah l'enregistrement des certificats d'économie d'énergie (CEE) générés par les travaux de rénovation thermique",
                "mettre en location ou continuer à louer votre bien décent et non meublé pour une durée minimum de 9 ans",
                "ne pas dépasser le montant de loyer maximal fixé localement par l'Anah",
                "louer, en tant que résidence principale, à des personnes dont les ressources sont inférieures aux plafonds fixés nationalement",
                "ne pas louer à des personnes de votre famille proche",
                "remettre le bien en location en cas de départ du locataire pendant la période couverte par la convention"
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );


        return new Subvention("prime Habiter Mieux", aide, conditions, infos);
    }

    private Subvention getProprietaireOccupantCITESubvention(final double reste, final int composition) {
        final double aide = getProprietaireOccupantCITE(reste, composition);
        final Set<String> conditions = Set.of(
                "vous n'avez pas bénéficié d'un PTZ (Prêt à taux zéro pour l'accession à la propriété) depuis 5 ans",
                /*"vos travaux doivent permettre de faire baisser votre consommation énergétique d'au moins 25 %",*/
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l’Anah l’enregistrement des Certificats d’Economie d’Energie (CEE) générés par les travaux de rénovation thermique",
                "vous engager à occuper le logement à titre de résidence principale pendant une durée minimale de 6 (six) ans après la fin des travaux."
        );
        final Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );

        return new Subvention("CITE", aide, conditions, infos);
    }


    private double getProprietaireOccupantAnah(final RevenusTypeEnum anahRevenusType, final double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return Math.round(Math.min(travauxPartHT * 50 / 100, 10000));
            case MODESTE:
                return Math.round(Math.min(travauxPartHT * 35 / 100, 7000));
            default:
                return 0;
        }
    }

    private double getProprietaireOccupantHabiterMieux(RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return Math.round(Math.min(travauxPartHT * 10 / 100, 2000));
            case MODESTE:
                return Math.round(Math.min(travauxPartHT * 10 / 100, 1600));
            default:
                return 0;
        }
    }

    private double getProprietaireOccupantCITE(final double reste, final int composition) {
        double plafond = getPlafond(composition);
        return Math.round(Math.min(reste, plafond) * 30 / 100);
    }

    private double getProprietaireOccupantWhitoutCITE(final int composition, final double total) {
        double plafond = getPlafond(composition);
        if (plafond * 30 / 100 + total > plafond) {
            return Math.round(plafond * 30 / 100 + total);
        }
        return Math.round(total / (1D - 30 / 100));
    }

    private double getPlafond(int composition) {
        double plafond;
        switch (composition) {
            case 1:
                plafond = 8000D;
                break;
            case 2:
                plafond = 16000D;
                break;
            default:
                plafond = 16000D + (composition - 2) * 400D;
        }
        return plafond;
    }


    private double getProprietaireOccupantDepartemental(final RevenusTypeEnum anahRevenusType, final double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return Math.round(Math.min(Math.min(travauxPartHT, 7000) * 60 / 100, 4200));
            case MODESTE:
                return Math.round(Math.min(Math.min(travauxPartHT, 7000) * 30 / 100, 2100));
            case INTERMEDIAIRE:
                return Math.round(Math.min(Math.min(travauxPartHT, 7000) * 15 / 100, 1050));
            default:
                return 0;
        }
    }

    private double getProprietaireBailleurAnah(final double travauxPartHT, final double surface) {
        return Math.round(Math.min(Math.min(travauxPartHT * 25 / 100, surface * 187.5), 15000));
    }

    private double getProprietaireBailleurHabiterMieux() {
        return 1500;
    }


}
