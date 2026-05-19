package com.finportfolio.entity;

public enum AlertCondition {
    PRICE_ABOVE,        // Fiyat su seviyeyi gecince
    PRICE_BELOW,        // Fiyat su seviyenin altina dusunce
    PERCENT_CHANGE_UP,  // Belirli yuzde artinca
    PERCENT_CHANGE_DOWN // Belirli yuzde azalinca
}