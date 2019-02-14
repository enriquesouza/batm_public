/*************************************************************************************
 * Copyright (C) 2014-2016 GENERAL BYTES s.r.o. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * GENERAL BYTES s.r.o.
 * Web      :  http://www.generalbytes.com
 *
 ************************************************************************************/
package com.generalbytes.batm.server.extensions.extra.smartcash;

import com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.exmo.*;
import com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.hitbtc.*;
import com.generalbytes.batm.server.extensions.*;
import com.generalbytes.batm.server.extensions.extra.smartcash.sources.smartcash.SmartCashRateSource;
import com.generalbytes.batm.server.extensions.extra.smartcash.wallets.smartcashd.SmartcashRPCWallet;

import org.knowm.xchange.exceptions.ExchangeException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartcashExtension extends AbstractExtension {

    private static final Logger log = LoggerFactory.getLogger("batm.master.ExmoExchange");
    private static final CryptoCurrencyDefinition DEFINITION = new SmartCashDefinition();
    public static final String CURRENCY = Currencies.SMART;

    @Override
    public String getName() {
        return "BATM " + Currencies.SMART + " extension";
    }

    @Override
    public IWallet createWallet(String walletLogin) {
        if (walletLogin != null && !walletLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(walletLogin, ":");
            String walletType = st.nextToken();
            if ("smartcashd".equalsIgnoreCase(walletType)) {
                // "bitcoind:protocol:user:password:ip:port:accountname"

                String protocol = st.nextToken();
                String username = st.nextToken();
                String password = st.nextToken();
                String hostname = st.nextToken();
                String port = st.nextToken();
                String accountName = "";
                if (st.hasMoreTokens()) {
                    accountName = st.nextToken();
                }

                if (protocol != null && username != null && password != null && hostname != null && port != null
                        && accountName != null) {
                    String rpcURL = protocol + "://" + username + ":" + password + "@" + hostname + ":" + port;
                    return new SmartCashRPCWallet(rpcURL, accountName);
                }
            }
        }
        return null;
    }

    @Override
    public Set<String> getSupportedCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CURRENCY);
        return result;
    }

    @Override
    public Set<ICryptoCurrencyDefinition> getCryptoCurrencyDefinitions() {
        Set<ICryptoCurrencyDefinition> result = new HashSet<>();
        result.add(DEFINITION);
        return result;
    }

    @Override
    public ICryptoAddressValidator createAddressValidator(String cryptoCurrency) {
        if (CURRENCY.equalsIgnoreCase(cryptoCurrency)) {
            return new SmartCashAddressValidator();
        }
        return null;
    }

    @Override
    public IPaperWalletGenerator createPaperWalletGenerator(String cryptoCurrency) {
        return new SmartCashWalletGenerator("", ctx);
    }

    @Override
    public IExchange createExchange(String paramString) throws ExchangeException {
        IExchange exchange = null;
        try {

            log.info("SmartCash Extension - LOG - " + paramString);

            if ((paramString != null) && (!paramString.trim().isEmpty())) {

                log.info("PARAM CREATE EXCHANGE - " + paramString);

                StringTokenizer paramTokenizer = new StringTokenizer(paramString, ":");

                String prefix = paramTokenizer.nextToken();

                log.info("PREFIX CREATE EXCHANGE - " + prefix);

                if ("exmo".toLowerCase().contains(prefix.toLowerCase())) {

                    String apiKey = paramTokenizer.nextToken();

                    log.info("apiKey CREATE EXCHANGE - " + apiKey);

                    String apiSecret = paramTokenizer.nextToken();

                    log.info("apiSecret CREATE EXCHANGE - " + apiSecret);

                    String preferredFiatCurrency = Currencies.EUR;

                    log.info("preferredFiatCurrency CREATE EXCHANGE - " + preferredFiatCurrency);

                    if (paramTokenizer.hasMoreTokens()) {
                        preferredFiatCurrency = paramTokenizer.nextToken().toUpperCase();
                    }

                    exchange = new ExmoExchange(apiKey, apiSecret, preferredFiatCurrency);

                    return exchange;

                } else if ("hitbtc".equalsIgnoreCase(prefix)) {
                    String preferredFiatCurrency = Currencies.USD;
                    String apiKey = paramTokenizer.nextToken();
                    String apiSecret = paramTokenizer.nextToken();
                    return new HitbtcExchange(apiKey, apiSecret, preferredFiatCurrency);
                }
            }
        } catch (ExchangeException ex) {

            log.error("ERROR CREATE EXCHANGE " + ex.getMessage());

            if (ex.getStackTrace() != null) {
                ex.printStackTrace();
            }

            throw new ExchangeException(ex.getMessage() + paramString);
        }
        return null;
    }

    @Override
    public IRateSource createRateSource(String sourceLogin) {

        if (sourceLogin != null && !sourceLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(sourceLogin, ":");
            String exchangeType = st.nextToken();

            if ("smartfix".equalsIgnoreCase(exchangeType)) {
                BigDecimal rate = BigDecimal.ZERO;
                if (st.hasMoreTokens()) {
                    try {
                        rate = new BigDecimal(st.nextToken());
                    } catch (Throwable e) {
                    }
                }
                String preferedFiatCurrency = Currencies.USD;
                if (st.hasMoreTokens()) {
                    preferedFiatCurrency = st.nextToken().toUpperCase();
                }
                return new FixPriceRateSource(rate, preferedFiatCurrency);
            } else if ("smartapi".equalsIgnoreCase(exchangeType)) {
                String preferredFiatCurrency = Currencies.USD;
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken();
                }
                return new SmartCashRateSource(preferredFiatCurrency);
            }

        }
        return null;
    }
}
