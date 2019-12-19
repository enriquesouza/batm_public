package com.generalbytes.batm.server.extensions.extra.smartcash.sources;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.generalbytes.batm.common.currencies.CryptoCurrency;
import com.generalbytes.batm.common.currencies.FiatCurrency;
import com.generalbytes.batm.server.extensions.IRateSource;

/**
 * Created by b00lean on 7/31/14.
 */
public class FixPriceRateSource implements IRateSource {
    private BigDecimal rate = BigDecimal.ZERO;

    private String preferedFiatCurrency = FiatCurrency.USD.getCode();

    public FixPriceRateSource(BigDecimal rate, String preferedFiatCurrency) {
        this.rate = rate;
        if (FiatCurrency.EUR.getCode().equalsIgnoreCase(preferedFiatCurrency)) {
            this.preferedFiatCurrency = FiatCurrency.EUR.getCode();
        }
        if (FiatCurrency.USD.getCode().equalsIgnoreCase(preferedFiatCurrency)) {
            this.preferedFiatCurrency = FiatCurrency.USD.getCode();
        }
    }

    @Override
    public Set<String> getCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CryptoCurrency.SMART.getCode());
        return result;
    }

    @Override
    public BigDecimal getExchangeRateLast(String cryptoCurrency, String fiatCurrency) {
        if (CryptoCurrency.SMART.getCode().equalsIgnoreCase(cryptoCurrency)) {
            return rate;
        }
        return null;
    }

    @Override
    public Set<String> getFiatCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(FiatCurrency.USD.getCode());
        result.add(FiatCurrency.EUR.getCode());
        return result;
    }

    @Override
    public String getPreferredFiatCurrency() {
        return preferedFiatCurrency;
    }
}
