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
                return computeRevenusType(revenus, 11522, 17283, 25771);
            }
            case 2: {
                return computeRevenusType(revenus, 16913, 25369, 36139);
            }
            case 3: {
                return computeRevenusType(revenus, 20312, 30467, 45066);
            }
            case 4: {
                return computeRevenusType(revenus, 23717, 35575, 51563);
            }
            case 5: {
                return computeRevenusType(revenus, 27132, 40699, 60578);
            }
            default: {
                return computeRevenusType(revenus, 27132 + (composition - 5) * 3412,
                        40699 + (composition - 5) * 5113,
                        60578 + (composition - 5) * 9477);
            }
        }
    }
}
