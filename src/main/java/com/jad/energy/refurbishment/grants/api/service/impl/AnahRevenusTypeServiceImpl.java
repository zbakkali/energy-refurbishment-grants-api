package com.jad.energy.refurbishment.grants.api.service.impl;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import org.springframework.stereotype.Service;

@Service
public class AnahRevenusTypeServiceImpl extends AbstractRevenusTypeServiceImpl {

    @Override
    public RevenusTypeEnum compute(double revenus, int composition) {
        switch (composition) {
            case 1: {
                return computeRevenusType(revenus, 19875, 24194);
            }
            case 2: {
                return computeRevenusType(revenus, 29171, 35510);
            }
            case 3: {
                return computeRevenusType(revenus, 35032, 42648);
            }
            case 4: {
                return computeRevenusType(revenus, 40905, 42648);
            }
            case 5: {
                return computeRevenusType(revenus, 46798, 56970);
            }
            default: {
                return computeRevenusType(revenus, 46798 + (composition - 5) * 5882,
                        56970 + (composition - 5) * 7162);
            }
        }
    }

}
