package com.jad.energy.refurbishment.grants.api.service;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;

public interface DepartementalRevenusTypeService {

    RevenusTypeEnum compute(final double revenus,
                            final int composition);
}
