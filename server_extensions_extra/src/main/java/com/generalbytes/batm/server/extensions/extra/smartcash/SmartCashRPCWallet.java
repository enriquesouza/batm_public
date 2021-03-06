/*************************************************************************************
 * Copyright (C) 2014-2018 GENERAL BYTES s.r.o. All rights reserved.
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
import com.generalbytes.batm.server.extensions.extra.common.RPCWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//You need to have node running: i.e.:  bitcoind -rpcuser=rpcuser -rpcpassword=rpcpassword -rpcport=8332

public class SmartCashRPCWallet extends RPCWallet {
    private static final Logger log = LoggerFactory.getLogger(SmartCashRPCWallet.class);

    public SmartCashRPCWallet(String rpcURL, String accountName) {
        super(rpcURL, accountName, CryptoCurrency.SMART.getCode());
        this.rpcURL = rpcURL;
        this.accountName = accountName;
    }


    private String rpcURL;
    private String accountName;

    @Override
    public Set<String> getCryptoCurrencies() {
        Set<String> result = new HashSet<String>();
        result.add(CryptoCurrency.SMART.getCode());
        return result;

    }

    @Override
    public String getPreferredCryptoCurrency() {
        return CryptoCurrency.SMART.getCode();
    }

    @Override
    public String sendCoins(String destinationAddress, BigDecimal amount, String cryptoCurrency, String description) {
        if (!CryptoCurrency.SMART.getCode().equalsIgnoreCase(cryptoCurrency)) {
            log.error("Smartcashd wallet error: unknown cryptocurrency.");
            return null;
        }
        String optionalPrefix = "smartcash:";
        if (destinationAddress.startsWith(optionalPrefix)) {
            destinationAddress = destinationAddress.substring(optionalPrefix.length(), destinationAddress.length());
        }

        log.info("Smartcashd sending coins from " + accountName + " to: " + destinationAddress + " " + amount);
        try {
            String result = getClient(rpcURL).sendFrom(accountName, destinationAddress, amount);
            log.debug("result = " + result);
            return result;
        } catch (BitcoinRPCException e) {
            log.error("Error", e);
            return null;
        }
    }

    @Override
    public String getCryptoAddress(String cryptoCurrency) {
        if (!CryptoCurrency.SMART.getCode().equalsIgnoreCase(cryptoCurrency)) {
            log.error("Smartcashd wallet error: unknown cryptocurrency.");
            return null;
        }

        try {
            List<String> addressesByAccount = getClient(rpcURL).getAddressesByAccount(accountName);
            if (addressesByAccount == null || addressesByAccount.size() == 0) {
                return null;
            }else{
                return addressesByAccount.get(0);
            }
        } catch (BitcoinRPCException e) {
            log.error("Error", e);
            return null;
        }
    }

    @Override
    public BigDecimal getCryptoBalance(String cryptoCurrency) {
        if (!CryptoCurrency.SMART.getCode().equalsIgnoreCase(cryptoCurrency)) {
            log.error("Smartcashd wallet error: unknown cryptocurrency: " + cryptoCurrency);
            return null;
        }
        try {
            return getClient(rpcURL).getBalance(accountName);
        } catch (BitcoinRPCException e) {
            log.error("Error", e);
            return null;
        }
    }

    private BitcoinJSONRPCClient getClient(String rpcURL) {
        try {
            return new BitcoinJSONRPCClient(rpcURL);
        } catch (MalformedURLException e) {
            log.error("Error", e);
        }
        return null;
    }

}
