package com.jad.energy.refurbishment.grants.api.service;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import com.jad.energy.refurbishment.grants.api.service.impl.AnahRevenusTypeServiceImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnahRevenusTypeServiceTest {

    private RevenusTypeService anahRevenusTypeService;

    @Before
    public void setUp() {
        anahRevenusTypeService = new AnahRevenusTypeServiceImpl();
    }

    @Test
    public void compute() throws Exception {
        // revenus fiscaux de référence de l'année n-2
        double revenus = 34940D;
        // Composition du foyer
        int composition = 3;

        assertEquals(
                RevenusTypeEnum.TRES_MODESTE,
                anahRevenusTypeService.compute(revenus, composition));
    }

}