package com.jad.energy.refurbishment.grants.api.service.impl;

import com.jad.energy.refurbishment.grants.api.model.RevenusTypeEnum;
import com.jad.energy.refurbishment.grants.api.service.RevenusTypeService;

public abstract class AbstractRevenusTypeServiceImpl implements RevenusTypeService {

    protected RevenusTypeEnum computeRevenusType(final double revenus, final double tresModesteMax, final double modesteMax) {
        if (revenus < tresModesteMax) {
            return RevenusTypeEnum.TRES_MODESTE;
        }
        if (revenus < modesteMax) {
            return RevenusTypeEnum.MODESTE;
        }
        return RevenusTypeEnum.NON_MODESTE;
    }

    protected RevenusTypeEnum computeRevenusType(final double revenus, final double tresModesteMax, final double modesteMax, final double intermediairesMax) {
        if (revenus < tresModesteMax) {
            return RevenusTypeEnum.TRES_MODESTE;
        }
        if (revenus < modesteMax) {
            return RevenusTypeEnum.MODESTE;
        }
        if(revenus < intermediairesMax) {
            return RevenusTypeEnum.INTERMEDIAIRE;
        }
        return RevenusTypeEnum.NON_MODESTE;
    }
}
