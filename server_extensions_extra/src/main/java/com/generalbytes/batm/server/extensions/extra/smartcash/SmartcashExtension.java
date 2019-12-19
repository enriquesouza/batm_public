/*************************************************************************************
 * Copyright (C) 2014-2019 GENERAL BYTES s.r.o. All rights reserved.
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

import com.generalbytes.batm.common.currencies.CryptoCurrency;
import com.generalbytes.batm.common.currencies.FiatCurrency;
import com.generalbytes.batm.server.extensions.*;
import com.generalbytes.batm.server.extensions.FixPriceRateSource;
import com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.exmo.ExmoExchange;
import com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.hitbtc.HitbtcExchange;
import com.generalbytes.batm.server.extensions.extra.smartcash.sources.smartcash.SmartCashRateSource;
//import com.generalbytes.batm.server.extensions.extra.smartcash.wallets.smartcashd.SmartcashRPCWallet;
import org.knowm.xchange.exceptions.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class SmartcashExtension extends AbstractExtension{

    private static final String SMARTAPI = "smartapi";
    private static final String SMARTFIX = "smartfix";
    private static final String HITBTC = "hitbtc";
    private static final String EXMO = "exmo";
    private static final String STRING = "";
    private static final String DELIM = ":";
    private static final String SMARTCASHD = "smartcashd";
    private static final Logger log = LoggerFactory.getLogger("batm.master.ExmoExchange");
    private static final CryptoCurrencyDefinition DEFINITION = new SmartCashDefinition();
    public static final String CURRENCY = CryptoCurrency.SMART.toString();

    @Override
    public String getName() {
        return "BATM Smartcash extension";
    }

    @Override
    public IWallet createWallet(String walletLogin, String tunnelPassword) {
        if (walletLogin !=null && !walletLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(walletLogin,":");
            String walletType = st.nextToken();

            if ("smartcashd".equalsIgnoreCase(walletType)) {
                //"smartcashd:protocol:user:password:ip:port:accountname"

                String protocol = st.nextToken();
                String username = st.nextToken();
                String password = st.nextToken();
                String hostname = st.nextToken();
                String port = st.nextToken();
                String accountName ="";
                if (st.hasMoreTokens()) {
                    accountName = st.nextToken();
                }


                if (protocol != null && username != null && password != null && hostname !=null && port != null && accountName != null) {
                    String rpcURL = protocol +"://" + username +":" + password + "@" + hostname +":" + port;
                    return new SmartCashRPCWallet(rpcURL,accountName);
                }
            }
            if ("smartdemo".equalsIgnoreCase(walletType)) {

                String fiatCurrency = st.nextToken();
                String walletAddress = "";
                if (st.hasMoreTokens()) {
                    walletAddress = st.nextToken();
                }

                if (fiatCurrency != null && walletAddress != null) {
                    return new DummyExchangeAndWalletAndSource(fiatCurrency, CryptoCurrency.SMART.getCode(), walletAddress);
                }
            }
        }
        return null;
    }

    @Override
    public ICryptoAddressValidator createAddressValidator(String cryptoCurrency) {
        if (CryptoCurrency.SMART.getCode().equalsIgnoreCase(cryptoCurrency)) {
            return new SmartCashAddressValidator();
        }
        return null;
    }

    @Override
    public IRateSource createRateSource(String sourceLogin) {
        if (sourceLogin != null && !sourceLogin.trim().isEmpty()) {
            StringTokenizer st = new StringTokenizer(sourceLogin,":");
            String exchangeType = st.nextToken();

            if ("smartfix".equalsIgnoreCase(exchangeType)) {
                BigDecimal rate = BigDecimal.ZERO;
                if (st.hasMoreTokens()) {
                    try {
                        rate = new BigDecimal(st.nextToken());
                    } catch (Throwable e) {
                    }
                }
                String preferedFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    preferedFiatCurrency = st.nextToken().toUpperCase();
                }
                return new FixPriceRateSource(rate,preferedFiatCurrency);
            }else if ("smartapi".equalsIgnoreCase(exchangeType)) {
                String preferredFiatCurrency = FiatCurrency.USD.getCode();
                if (st.hasMoreTokens()) {
                    preferredFiatCurrency = st.nextToken();
                }
                return new SmartCashRateSource(preferredFiatCurrency);
            }

        }
        return null;
    }

    @Override
    public Set<String> getSupportedCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CryptoCurrency.SMART.getCode());
        return result;
    }

    @Override
    public Set<ICryptoCurrencyDefinition> getCryptoCurrencyDefinitions() {
        Set<ICryptoCurrencyDefinition> result = new HashSet<>();
        result.add(DEFINITION);
        return result;
    }

    @Override
    public IExchange createExchange(String paramString) throws ExchangeException {
        IExchange exchange = null;
        try {

            log.info("SmartCash Extension - LOG - " + paramString);

            if ((paramString != null) && (!paramString.trim().isEmpty())) {

                log.info("PARAM CREATE EXCHANGE - " + paramString);

                StringTokenizer paramTokenizer = new StringTokenizer(paramString, DELIM);

                String prefix = paramTokenizer.nextToken();

                log.info("PREFIX CREATE EXCHANGE - " + prefix);

                if (EXMO.toLowerCase().contains(prefix.toLowerCase())) {

                    String apiKey = paramTokenizer.nextToken();

                    log.info("apiKey CREATE EXCHANGE - " + apiKey);

                    String apiSecret = paramTokenizer.nextToken();

                    log.info("apiSecret CREATE EXCHANGE - " + apiSecret);

                    String preferredFiatCurrency = FiatCurrency.EUR.getCode();

                    log.info("preferredFiatCurrency CREATE EXCHANGE - " + preferredFiatCurrency);

                    if (paramTokenizer.hasMoreTokens()) {
                        preferredFiatCurrency = paramTokenizer.nextToken().toUpperCase();
                    }

                    exchange = new ExmoExchange(apiKey, apiSecret, preferredFiatCurrency);

                    return exchange;

                } else if (HITBTC.equalsIgnoreCase(prefix)) {
                    String preferredFiatCurrency = FiatCurrency.USD.getCode();
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
    public IPaperWalletGenerator createPaperWalletGenerator(String cryptoCurrency) {
        log.info("INFO - SMART - PAPER WALLET");
        if (CURRENCY.equalsIgnoreCase(cryptoCurrency)) {
            return new SmartCashWalletGenerator(STRING, ctx);
        }
        return null;
    }
}
