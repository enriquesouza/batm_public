package com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.hitbtc;

import java.util.HashSet;
import java.util.Set;

import com.generalbytes.batm.server.extensions.Currencies;
import com.generalbytes.batm.server.extensions.extra.bitcoin.exchanges.XChangeExchange;

import org.knowm.xchange.ExchangeSpecification;

public class HitbtcExchange extends XChangeExchange {

    public HitbtcExchange(String preferredFiatCurrency) {
        super(getDefaultSpecification(), preferredFiatCurrency);
    }

    public HitbtcExchange(String clientKey, String clientSecret, String preferredFiatCurrency) {
        super(getSpecification(clientKey, clientSecret), preferredFiatCurrency);
    }

    private static ExchangeSpecification getDefaultSpecification() {
        return new org.knowm.xchange.hitbtc.v2.HitbtcExchange().getDefaultExchangeSpecification();
    }

    private static ExchangeSpecification getSpecification(String clientKey, String clientSecret) {
        ExchangeSpecification spec = getDefaultSpecification();
        spec.setApiKey(clientKey);
        spec.setSecretKey(clientSecret);
        return spec;
    }

    @Override
    public Set<String> getCryptoCurrencies() {
        Set<String> cryptoCurrencies = new HashSet<>();
        cryptoCurrencies.add(Currencies.BTC);

        cryptoCurrencies.add(Currencies.SMART);

        return cryptoCurrencies;
    }

    @Override
    public Set<String> getFiatCurrencies() {
        Set<String> fiatCurrencies = new HashSet<>();
        fiatCurrencies.add(Currencies.USD);
        return fiatCurrencies;
    }

    @Override
    protected boolean isWithdrawSuccessful(String result) {
        return true;
    }

    @Override
    protected double getAllowedCallsPerSecond() {
        return 10;
    }

    // public static void main(String[] args) {
    // HitbtcExchange xch = new HitbtcExchange("", "", "USD");
    // System.out.println(xch.getDepositAddress("XMR"));
    // System.out.println(xch.getExchangeRateForSell("XMR", "USD"));
    // //System.out.println(xch.sellCoins(BigDecimal.TEN, "XMR", "USD", ""));
    // System.out.println(xch.sendCoins("86didNu7QQdJvm1CAxpUCy9rJr7AcRLdz1xzSMEFio8DVknAu3PoLkY7VNoDBFdM2ZZ4kzfKyrHEUHrjRauXwSZGJ7SA7Ki",
    // BigDecimal.TEN, "XMR", ""));
    // }
}