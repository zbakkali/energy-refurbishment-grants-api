package com.jad.energy.refurbishment.grants.api.service.impl;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import org.springframework.stereotype.Service;

@Service
public class AnahRevenusTypeServiceImpl extends AbstractRevenusTypeServiceImpl {

    @Override
    public RevenusTypeEnum compute(double revenus, int composition) {
        switch (composition) {
            case 1: {
                return computeRevenusType(revenus, 20079, 24443);
            }
            case 2: {
                return computeRevenusType(revenus, 29471, 35875);
            }
            case 3: {
                return computeRevenusType(revenus, 35392, 43086);
            }
            case 4: {
                return computeRevenusType(revenus, 41325, 50311);
            }
            case 5: {
                return computeRevenusType(revenus, 47279, 57555);
            }
            default: {
                return computeRevenusType(revenus, 47279 + (composition - 5) * 5943,
                        57555 + (composition - 5) * 7236);
            }
        }
    }

}
