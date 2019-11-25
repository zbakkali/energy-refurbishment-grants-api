package com.jad.energy.refurbishment.grants.api.service.impl;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import com.jad.energy.refurbishment.grants.api.service.RevenusTypeService;
import org.springframework.stereotype.Service;

@Service
public class DepartementalRevenusTypeServiceImpl extends AbstractRevenusTypeServiceImpl implements RevenusTypeService {
    @Override
    public RevenusTypeEnum compute(double revenus, int composition) {
        switch (composition) {
            case 1: {
                return computeRevenusType(revenus, 11_640, 17_460, 26_035);
            }
            case 2: {
                return computeRevenusType(revenus, 17_087, 25_629, 36_510);
            }
            case 3: {
                return computeRevenusType(revenus, 20_520, 30_780, 45_528);
            }
            case 4: {
                return computeRevenusType(revenus, 23_960, 35_940, 52_092);
            }
            case 5: {
                return computeRevenusType(revenus, 27_410, 41_117, 61_200);
            }
            default: {
                return computeRevenusType(revenus, 27_410 + (composition - 5) * 3_447,
                        41_117 + (composition - 5) * 5_165,
                        61_200 + (composition - 5) * 9_574);
            }
        }
    }
}
