package com.jad.energy.refurbishment.grants.api.controller;

import com.jad.energy.refurbishment.grants.api.model.*;
import com.jad.energy.refurbishment.grants.api.service.RevenusTypeService;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@RefreshScope
@RestController
public class SubventionController {

    private static Logger LOGGER = LoggerFactory.getLogger(SubventionController.class);

    @Value("${message:Hello default}")
    private String message;

    @GetMapping("/message")
    String getMessage() {
        LOGGER.info("Handling message");
        return this.message;
    }

    private final RevenusTypeService anahRevenusTypeServiceImpl;
    private final RevenusTypeService departementalRevenusTypeServiceImpl;

    public SubventionController(RevenusTypeService anahRevenusTypeServiceImpl,
                                RevenusTypeService departementalRevenusTypeServiceImpl) {

        this.anahRevenusTypeServiceImpl = anahRevenusTypeServiceImpl;
        this.departementalRevenusTypeServiceImpl = departementalRevenusTypeServiceImpl;
    }

    @GetMapping("compute_anah_revenus_type")
    public RevenusTypeEnum computeAnahRevenusType(@ApiParam(value = "${revenus.description}") @RequestParam double revenus,
                                                  @ApiParam(value = "${composition.description}") @RequestParam int composition,
                                                  @ApiParam(value = "${requesterType.description}") @RequestParam RequesterTypeEnum requesterType) {

        // plafonds
        return anahRevenusTypeServiceImpl.compute(revenus, composition);
    }

    @CrossOrigin
    @GetMapping("compute_subvention")
    public List<ScenarioSubvention> computeSubvention(@RequestParam double revenus,
                                                      @RequestParam long quotepart1,
                                                      @RequestParam long quotepart2,
                                                      @RequestParam int composition,
                                                      @RequestParam RequesterTypeEnum requesterType,
                                                      @RequestParam(required = false) Double surface) {

        /*Map<Double, Double> scenariosTravauxTTC = Map.of(
                3628467D, 137293D,
                6131351D, 91447D,
                6440118D, 131925D
        );*/

        Set<Scenario> scenarios = Set.of(
                new Scenario(
                        "Scenario 1",
                        "Ravalement de façade avec isolation",
                        25,
                        3_628_467D,
                        168_331D,
                        0, // demande de devis ?
                        0,
                        0
                ),
                new Scenario(
                        "Scenario 2",
                        "Ravalement de façade avec isolation + Changement des menuiseries",
                        38,
                        5_224_547D,
                        246_942D,
                        49_344D,
                        0,
                        0
                ),
                new Scenario(
                        "Scenario 3",
                        "Ravalement de façade avec isolation + Changement des menuiseries + Rénovation du système de ventilation",
                        42,
                        5_542_947D,
                        228_004D,
                        49_344D,
                        0,
                        0
                )
        );

        double travauxTva = 5.5 / 100;
        double amoTva = 20.0 / 100;
        int habitationCount = 398;

        // Double quotePart = 344D / 200000;
        Double quotePart = 1D * quotepart1 / quotepart2;


        return scenarios.stream()
                .map(scenario -> {
                    double travauxPartTTC = round((scenario.getCoutCoproprieteTravaux() + scenario.getCoutCoproprieteMOE()) * quotePart);
                    double travauxPartHT = travauxPartTTC * (1 - travauxTva);

                    double amoPartTTC = round(scenario.getCoutCoproprieteAMO() * quotePart);
                    double amoPartHT = amoPartTTC * (1 - amoTva);

                    Optional<Subvention> habiterMieuxCoproTravauxSubvention = getHabiterMieuxCoproTravauxSubvention(travauxPartHT, scenario.getGainEnergetique());
                    Optional<Subvention> primeHabiterMieuxCoproTravauxSubvention = getPrimeHabiterMieuxCoproTravauxSubvention(scenario.getGainEnergetique());
                    Optional<Subvention> habiterMieuxCoproAMOSubvention = getHabiterMieuxCoproAMOSubvention(amoPartHT, scenario.getGainEnergetique());


                    switch (requesterType) {
                        case PROPRIETAIRE_OCCUPANT: {
                            RevenusTypeEnum anahRevenusType = anahRevenusTypeServiceImpl.compute(revenus, composition);
                            RevenusTypeEnum departementalRevenusType = departementalRevenusTypeServiceImpl.compute(revenus, composition);

                            Subvention habiterMieuxSubvention = getProprietaireOccupantHabiterMieuxSubvention(anahRevenusType, travauxPartHT);
                            Subvention primeHabiterMieuxSubvention = getProprietaireOccupantPrimeHabiterMieuxSubvention(anahRevenusType, travauxPartHT);
                            Subvention habiterMieuxAMOSubvention = getProprietaireOccupantHabiterMieuxAMOSubvention(amoPartHT);

                            //double aideAccompagnementConseil = getProprietaireOccupantAnahAideAccompagnementConseil();
                            //Subvention ceeSubvention = new Subvention("cee", round(travauxTotal.getValue() * quotePart), null, null);


                            double fixedSubvention = habiterMieuxCoproAMOSubvention.map(Subvention::getMontant).orElse(0D) +
                                    habiterMieuxCoproTravauxSubvention.map(Subvention::getMontant).orElse(0D) +
                                    habiterMieuxSubvention.getMontant() +
                                    primeHabiterMieuxSubvention.getMontant() /*+ ceeSubvention.getMontant()*/;

                            Subvention departementaleSubvention = getProprietaireOccupantDepartementalSubvention(departementalRevenusType, travauxPartHT);

                            double reste = travauxPartTTC - fixedSubvention - departementaleSubvention.getMontant();
                            Subvention citeSubvention = getProprietaireOccupantCITESubvention(reste, composition);
                            double cout = round(reste - citeSubvention.getMontant());

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
                                cout = round(reste - citeSubvention.getMontant());
                            }

                            return new ScenarioSubvention(
                                    scenario,
                                    travauxPartTTC,
                                    cout,
                                    List.of(
                                            habiterMieuxCoproAMOSubvention.orElse(null),
                                            habiterMieuxSubvention,
                                            primeHabiterMieuxSubvention
                                            /*, ceeSubvention*/,
                                            departementaleSubvention,
                                            habiterMieuxSubvention,
                                            primeHabiterMieuxSubvention,
                                            citeSubvention));
                        }
                        case PROPRIETAIRE_BAILLEUR: {
                            double gainEnergetique = scenario.getGainEnergetique();
                            Subvention anahSubvention = getProprietaireBailleurAnahSubvention(travauxPartHT, surface, gainEnergetique);
                            Subvention habiterMieuxSubvention = getProprietaireBailleurHabiterMieuxSubvention(gainEnergetique);
                            //Subvention ceeSubvention = new Subvention("cee", round(travauxTotal.getValue() * quotePart), null, null);

                            double fixedSubvention = habiterMieuxCoproAMOSubvention.map(Subvention::getMontant).orElse(0D) +
                                    habiterMieuxCoproTravauxSubvention.map(Subvention::getMontant).orElse(0D) +
                                    anahSubvention.getMontant() +
                                    habiterMieuxSubvention.getMontant();
                                    //ceeSubvention.getMontant() +;

                            double cout = travauxPartTTC - fixedSubvention;

                            return new ScenarioSubvention(
                                    scenario,
                                    travauxPartTTC,
                                    cout,
                                    List.of(
                                            habiterMieuxSubvention,
                                            anahSubvention,
                                            //ceeSubvention,
                                            new Subvention("aide départementale à l'amélioration de l'habitat privé", 0, null, null),
                                            //habiterMieuxCoproSubvention,
                                            new Subvention("CITE", 0, null, null)
                                    ));
                        }
                    }
                    return null;
                }).collect(toList());
    }

    private Subvention getProprietaireOccupantHabiterMieuxAMOSubvention(double amoPartHT) {
        double aide = 560;
        return new Subvention("Habiter Mieux AMO", aide, null, null);
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

    private static Optional<Subvention> getHabiterMieuxCoproTravauxSubvention(double travauxPartHT, double gainEnergetique) {
        if (gainEnergetique >= 35) {
            double aide = round(min(travauxPartHT, 15000) * 25 / 100);
            return of(new Subvention("Habiter Mieux Copropriétés", aide, null, null));
        }
        return empty();
    }

    private Optional<Subvention> getPrimeHabiterMieuxCoproTravauxSubvention(double gainEnergetique) {
        if (gainEnergetique >= 35) {
            double aide = 1500;
            return of(new Subvention("Prime Habiter Mieux Copropriétés", aide, null, null));
        }
        return empty();
    }

    private static Optional<Subvention> getHabiterMieuxCoproAMOSubvention(double amoPartHT, double gainEnergetique) {
        if (gainEnergetique >= 35) {
            double aide = round(min(amoPartHT, 600) * 30 / 100);
            return of(new Subvention("Habiter Mieux Copropriétés AMO", aide, null, null));
        }
        return empty();
    }

    private Subvention getProprietaireOccupantHabiterMieuxSubvention(RevenusTypeEnum anahRevenusType,
                                                                     double travauxPartHT) {
        final double aide = getProprietaireOccupantHabiterMieux(anahRevenusType, travauxPartHT);
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

    private Subvention getProprietaireOccupantPrimeHabiterMieuxSubvention(RevenusTypeEnum anahRevenusType,
                                                                          double travauxPartHT) {
        final double aide = getProprietaireOccupantPrimeHabiterMieux(anahRevenusType, travauxPartHT);
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

    private Subvention getProprietaireOccupantDepartementalSubvention(RevenusTypeEnum anahRevenusType,
                                                                      double travauxPartHT) {
        return getProprietaireOccupantDepartementalSubvention(anahRevenusType, travauxPartHT, null);
    }


    private Subvention getProprietaireOccupantDepartementalSubvention(RevenusTypeEnum anahRevenusType,
                                                                      double travauxPartHT, Double departementaleSubventionMontant) {
        double aide = departementaleSubventionMontant != null ? departementaleSubventionMontant : getProprietaireOccupantDepartemental(anahRevenusType, travauxPartHT);
        Set<String> conditions = Set.of(
                "vous n'avez pas bénéficié d'un PTZ (Prêt à taux zéro pour l'accession à la propriété) depuis 5 ans",
                "les travaux ne peuvent commencer qu'à partir de la réception de la décision vous attribuant la subvention.",
                "Réserver à l’Anah l’enregistrement des Certificats d’Economie d’Energie (CEE) générés par les travaux de rénovation thermique",
                "vous engager à occuper le logement à titre de résidence principale pendant une durée minimale de 6 (six) ans après la fin des travaux."
        );
        Set<String> infos = Set.of(
                "l'aide de l'Anah n'est pas un droit. La décision d'attribution est faite au niveau local selon des priorités et des moyens propres.",
                "la subvention est versée une fois les travaux achevés. Toutefois, il est possible de bénéficier du versement d'une avance lorsque les travaux n'ont pas encore démarré et qu'aucune somme n'a été versée à l'entreprise chargée de les exécuter"
        );

        return new Subvention("aide départementale à l'amélioration de l'habitat privé", aide, conditions, infos);

    }


    private Subvention getProprietaireBailleurAnahSubvention(double travauxPartHT, double surface, double gainEnergetique) {

        final double aide = getProprietaireBailleurAnah(travauxPartHT, surface, gainEnergetique);
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


    private Subvention getProprietaireBailleurHabiterMieuxSubvention(double gainEnergetique) {
        final double aide = getProprietaireBailleurHabiterMieux(gainEnergetique);
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

    private Subvention getProprietaireOccupantCITESubvention(double reste, int composition) {
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


    private double getProprietaireOccupantHabiterMieux(RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return round(min(travauxPartHT, 20000) * 50 / 100);
            case MODESTE:
                return round(min(travauxPartHT, 20000) * 35 / 100);
            default:
                return 0;
        }
    }

    private double getProprietaireOccupantPrimeHabiterMieux(RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return round(min(travauxPartHT * 10 / 100, 2000));
            case MODESTE:
                return round(min(travauxPartHT * 10 / 100, 1600));
            default:
                return 0;
        }
    }

    private double getProprietaireOccupantHabiterMieuxSerenite(RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        return getProprietaireOccupantHabiterMieux(anahRevenusType, travauxPartHT)
                + getProprietaireOccupantPrimeHabiterMieux(anahRevenusType, travauxPartHT);
    }

    private double getProprietaireOccupantAnahAideAccompagnementConseil() {
        return 560;
    }

    private double getProprietaireOccupantCITE(double reste, int composition) {
        double plafond = getPlafond(composition);
        return round(min(reste, plafond) * 30 / 100);
    }

    private double getProprietaireOccupantWhitoutCITE(int composition, double total) {
        double plafond = getPlafond(composition);
        if (plafond * 30 / 100 + total > plafond) {
            return round(plafond * 30 / 100 + total);
        }
        return round(total / (1D - 30 / 100));
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


    private double getProprietaireOccupantDepartemental(RevenusTypeEnum anahRevenusType, double travauxPartHT) {
        switch (anahRevenusType) {
            case TRES_MODESTE:
                return round(min(min(travauxPartHT, 7000) * 60 / 100, 4200));
            case MODESTE:
                return round(min(min(travauxPartHT, 7000) * 30 / 100, 2100));
            case INTERMEDIAIRE:
                return round(min(min(travauxPartHT, 7000) * 15 / 100, 1050));
            default:
                return 0;
        }
    }

    private double getProprietaireBailleurAnah(double travauxPartHT, double surface, double gainEnergetique) {
        if (gainEnergetique >= 35) {
            return round(min(min(travauxPartHT, 750 * surface), 60000) * 25 / 100);
        }
        return 0;
    }

    private double getProprietaireBailleurHabiterMieux(double gainEnergetique) {
        if (gainEnergetique >= 35) {
            return 1500;
        }
        return 0;
    }


}
