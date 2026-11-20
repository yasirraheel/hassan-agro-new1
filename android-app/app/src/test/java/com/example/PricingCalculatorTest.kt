package com.example

import com.example.data.local.PricingMarginRules
import com.example.data.local.calculateSuggestedTierPrices
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingCalculatorTest {

    @Test
    fun calculatesFourTiersFromPurchaseCostAndPercentProfitMargin() {
        val rules = PricingMarginRules(
            ownerMarginMode = "PERCENT",
            ownerMarginValue = 10.0,
            distributorStep = 0.0,
            wholesalerStep = 5.0,
            retailStep = 15.0,
            mrpStep = 25.0
        )

        val result = calculateSuggestedTierPrices(
            purchasePrice = 1000.0,
            taxRatePercent = 0.0,
            taxIncludedInCost = false,
            marginMode = rules.ownerMarginMode,
            marginValue = rules.ownerMarginValue,
            rules = rules
        )

        // Base floor = 1000 * 1.10 = 1100
        assertEquals(1000.0, result.landedCost, 0.01)
        assertEquals(1100.0, result.baseFloorPrice, 0.01)
        assertEquals(1100.0, result.distributorPrice, 0.01)
        assertEquals(1155.0, result.wholesalerPrice, 0.01)
        assertEquals(1265.0, result.retailPrice, 0.01)
        assertEquals(1375.0, result.mrpPrice, 0.01)
    }

    @Test
    fun calculatesFourTiersWithExcludedTaxAndFlatProfitMargin() {
        val rules = PricingMarginRules(
            ownerMarginMode = "FLAT",
            ownerMarginValue = 100.0,
            distributorStep = 0.0,
            wholesalerStep = 10.0,
            retailStep = 20.0,
            mrpStep = 30.0
        )

        val result = calculateSuggestedTierPrices(
            purchasePrice = 1000.0,
            taxRatePercent = 18.0,
            taxIncludedInCost = false,
            marginMode = rules.ownerMarginMode,
            marginValue = rules.ownerMarginValue,
            rules = rules
        )

        // Landed cost = 1000 * 1.18 = 1180; Base floor = 1180 + 100 = 1280
        assertEquals(1180.0, result.landedCost, 0.01)
        assertEquals(1280.0, result.baseFloorPrice, 0.01)
        assertEquals(1280.0, result.distributorPrice, 0.01)
        assertEquals(1408.0, result.wholesalerPrice, 0.01)
        assertEquals(1536.0, result.retailPrice, 0.01)
        assertEquals(1664.0, result.mrpPrice, 0.01)
    }
}
