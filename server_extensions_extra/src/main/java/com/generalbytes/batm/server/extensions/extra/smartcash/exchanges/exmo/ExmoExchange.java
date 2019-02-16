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
 * Other information:
 *
 * This implementation was created in cooperation with Orillia BVBA
 ************************************************************************************/
package com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.exmo;

import java.math.BigDecimal;
import java.util.*;

import com.generalbytes.batm.server.coinutil.DDOSUtils;
import com.generalbytes.batm.server.extensions.*;
import com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.lib.exmo.Exmo;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.ExchangeException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExmoExchange implements IExchangeAdvanced, IRateSourceAdvanced {

    private static final Logger log = LoggerFactory.getLogger("batm.master.ExmoExchange");

    private Exmo exchange = null;
    private String apiKey;
    private String apiSecret;
    private String preferredFiatCurrency;

    private static final HashMap<String, BigDecimal> rateAmounts = new HashMap<String, BigDecimal>();
    private static HashMap<String, Long> rateTimes = new HashMap<String, Long>();
    private static final long MAXIMUM_ALLOWED_TIME_OFFSET = 30 * 1000;

    public ExmoExchange(String apiKey, String apiSecret, String preferredFiatCurrency) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.preferredFiatCurrency = preferredFiatCurrency;
    }

    private synchronized Exmo getExchange() {

        if (this.exchange == null)
            this.exchange = new com.generalbytes.batm.server.extensions.extra.smartcash.exchanges.lib.exmo.Exmo(
                    this.apiKey, this.apiSecret);

        log.debug("Creating the exchange..." + this.exchange.toString());

        return this.exchange;
    }

    @Override
    public Set<String> getCryptoCurrencies() {
        Set<String> c = new HashSet<String>();
        c.add("BTC");
        c.add("SMART");
        return c;
    }

    @Override
    public Set<String> getFiatCurrencies() {
        Set<String> c = new HashSet<String>();
        c.add("USD");
        c.add("EUR");
        return c;
    }

    private boolean isCurrencyPairSupported(CurrencyPair pair) {
        return true;
    }

    @Override
    public String getPreferredFiatCurrency() {
        return preferredFiatCurrency;
    }

    @Override
    public synchronized BigDecimal getExchangeRateLast(String cryptoCurrency, String fiatCurrency) {
        String key = cryptoCurrency + "_" + fiatCurrency;
        synchronized (rateAmounts) {
            long now = System.currentTimeMillis();
            BigDecimal amount = rateAmounts.get(key);
            if (amount == null) {
                BigDecimal result = getExchangeRateLastSync(cryptoCurrency, fiatCurrency);
                log.debug("Called Exmo exchange for rate: " + key + " = " + result);
                rateAmounts.put(key, result);
                rateTimes.put(key, now + MAXIMUM_ALLOWED_TIME_OFFSET);
                return result;
            } else {
                Long expirationTime = rateTimes.get(key);
                if (expirationTime > now) {
                    return rateAmounts.get(key);
                } else {
                    // do the job;
                    BigDecimal result = getExchangeRateLastSync(cryptoCurrency, fiatCurrency);
                    log.debug("Called Exmo exchange for rate: " + key + " = " + result);
                    rateAmounts.put(key, result);
                    rateTimes.put(key, now + MAXIMUM_ALLOWED_TIME_OFFSET);
                    return result;
                }
            }
        }
    }

    private BigDecimal getExchangeRateLastSync(String cryptoCurrency, String cashCurrency) {

        try {
            DDOSUtils.waitForPossibleCall(getClass());

            Map<String, String> params = new HashMap<String, String>();
            String request = getExchange().Request("ticker", params);

            JSONObject tickers = new JSONObject(request).getJSONObject(cryptoCurrency + "_" + cashCurrency);

            log.debug("getExchangeRateLastSync - last - trade");

            return tickers.getBigDecimal("last_trade");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private BigDecimal getGenericBalance(String currency) {

        try {
            DDOSUtils.waitForPossibleCall(getClass());

            Map<String, String> params = new HashMap<String, String>();
            String request = getExchange().Request("user_info", params);

            log.debug("getGenericBalance" + request);

            JSONObject obj = new JSONObject(request);

            return obj.getJSONObject("balances").getBigDecimal(currency);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exmo exchange (getBalance) failed with message: " + e.getMessage());
        }
        return null;
    }

    @Override
    public BigDecimal getCryptoBalance(String cryptoCurrency) {

        return getGenericBalance(cryptoCurrency);
    }

    @Override
    public BigDecimal getFiatBalance(String fiatCurrency) {

        return getGenericBalance(fiatCurrency);

    }

    @Override
    public final String sendCoins(String destinationAddress, BigDecimal amount, String cryptoCurrency,
            String description) throws ExchangeException {

        log.info("Exchange withdrawing {} {} to {}", amount, cryptoCurrency, destinationAddress);

        DDOSUtils.waitForPossibleCall(getClass());
        Map<String, String> params = new HashMap<String, String>();
        params.put("amount", String.valueOf(amount));
        params.put("currency", cryptoCurrency);
        params.put("address", destinationAddress);

        String request = getExchange().Request("withdraw_crypt", params);

        JSONObject result = new JSONObject(request);

        try {
            if (result.has("result") && result.getBoolean("result")) {
                log.debug("exchange withdrawal completed with result: {}", result.getBoolean("result"));
                return "success";
            } else {
                log.error("exchange withdrawal failed with result: '{}'", result.getString("error"));
            }
        } catch (Exception e) {
            log.error("exchange withdrawal failed", e);
        }
        return null;
    }

    private JSONArray getOpenOrders(String currencyPair) {

        HashMap<String, String> paramsOpenOrders = new HashMap<String, String>();

        String request = getExchange().Request("user_open_orders", paramsOpenOrders);

        JSONObject orders = new JSONObject(request);

        if (!orders.has(currencyPair) || orders.isNull(currencyPair))
            return null;
        else
            return orders.getJSONArray(currencyPair);

    }

    private JSONObject openOrder(String currencyPair, BigDecimal amount, String type) {

        JSONObject order = null;
        HashMap<String, String> params = null;
        String request = null;

        try {

            params = new HashMap<String, String>();
            params.put("pair", currencyPair);
            params.put("quantity", amount.toString());
            params.put("type", type);
            params.put("price", "0");

            request = getExchange().Request("order_create", params);

            System.out.println(request);

            order = new JSONObject(request);

        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return order;

    }

    private boolean isOrderStillOpen(String orderId, String currencyPair) {

        boolean orderProcessed = false;

        boolean orderFound = false;

        JSONArray openOrders = getOpenOrders(currencyPair);

        if (openOrders == null) {

            orderProcessed = true;

        } else {

            DDOSUtils.waitForPossibleCall(getClass());

            for (int i = 0; i < openOrders.length(); i++) {

                JSONObject openOrder = openOrders.getJSONObject(i);

                log.debug("openOrder = " + openOrder);

                if (orderId.equals(openOrder.getString("order_id"))) {
                    orderFound = true;
                    break;
                }
            }
        }

        if (orderFound) {

            log.debug("Waiting for order to be processed.");

            try {
                Thread.sleep(3000);

                // don't get your ip address banned

            } catch (InterruptedException e) {

                e.printStackTrace();

            }
        } else {

            orderProcessed = true;

        }

        return orderProcessed ? false : true;
    }

    @Override
    public String purchaseCoins(BigDecimal amount, String cryptoCurrency, String fiatCurrencyToUse,
            String description) {

        String orderId = null;
        String currencyPair = cryptoCurrency + "_" + fiatCurrencyToUse;
        JSONObject order = null;
        boolean result_order = false;

        log.info("Calling Exmo exchange (purchase " + amount + " " + cryptoCurrency + ")");

        try {

            try {

                DDOSUtils.waitForPossibleCall(getClass());

                BigDecimal calculatedBuyPrice = calculateBuyPrice(cryptoCurrency, fiatCurrencyToUse, amount);

                BigDecimal fiatBalance = getFiatBalance(fiatCurrencyToUse);

                if (fiatBalance.compareTo(calculatedBuyPrice) < 0) {
                    System.out.print("Saldo insuficiente: ");
                    System.out.print("balance : " + fiatBalance);
                    System.out.print("valor necessário: " + calculatedBuyPrice);
                    return null;
                }

                order = openOrder(currencyPair, amount, "market_buy");

                if (order == null) {
                    System.out.println("Não encontramos o objeto result");
                    log.error("Não encontramos o objeto result");
                    return null;
                }

                if (!order.has("result") || order.isNull("result")) {
                    System.out.println("Não encontramos o objeto result");
                    log.error("Não encontramos o objeto result");
                    return null;
                }

                result_order = order.getBoolean("result");

                if (!result_order) {
                    System.out.println("O object result está falso");
                    log.error("O object result está falso");
                    return null;
                }

                if (!order.has("order_id") || order.isNull("order_id")) {
                    System.out.println("Não encontramos o objeto order_id");
                    log.error("Não encontramos o objeto order_id");
                    return null;
                }

                if (order.has("error") && !order.isNull("error") && !order.getString("error").isEmpty()) {
                    System.out.println("ERROR: " + order.getString("error"));
                    log.error("ERROR: " + order.getString("error"));
                    return null;
                }

                orderId = String.valueOf(order.get("order_id"));

                log.debug("orderId = " + orderId + " " + order);

                log.debug("purchaseCoins - order_create ");

                // log.debug("marketOrder = " + order.);

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(2000); // give exchange 2 seconds to reflect open order in order book
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // get open orders log.debug("Open orders:");

            boolean orderProcessed = false;
            int numberOfChecks = 0;
            while (!orderProcessed && numberOfChecks < 10) {

                boolean orderFound = false;

                JSONArray openOrders = getOpenOrders(currencyPair);

                if (openOrders == null) {
                    orderProcessed = true;
                } else {

                    DDOSUtils.waitForPossibleCall(getClass());

                    for (int i = 0; i < openOrders.length(); i++) {

                        JSONObject openOrder = openOrders.getJSONObject(i);

                        log.debug("openOrder = " + openOrder);

                        if (orderId.equals(openOrder.getString("order_id"))) {
                            orderFound = true;
                            break;
                        }
                    }
                }
                if (orderFound) {
                    log.debug("Waiting for order to be processed.");
                    try {
                        Thread.sleep(3000);

                        // don't get your ip address banned

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    orderProcessed = true;
                }
                numberOfChecks++;
            }
            if (orderProcessed) {
                return String.valueOf(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exmo exchange (purchaseCoins) failed with message: " + e.getMessage());
        }
        return null;

    }

    @Override
    public ITask createPurchaseCoinsTask(BigDecimal amount, String cryptoCurrency, String fiatCurrencyToUse,
            String description) {

        log.debug("SMART CASH - createPurchaseCoinsTask ");

        CurrencyPair currencyPair = new CurrencyPair(cryptoCurrency, fiatCurrencyToUse);

        if (!isCurrencyPairSupported(currencyPair)) {
            return null;
        }
        return new PurchaseCoinsTask(amount, cryptoCurrency, fiatCurrencyToUse, description);
    }

    @Override
    public String getDepositAddress(String cryptoCurrency) {
        if (!getCryptoCurrencies().contains(cryptoCurrency)) {
            log.error("Exmo implementation supports only " + Arrays.toString(getCryptoCurrencies().toArray()));
            return null;
        }
        try {
            DDOSUtils.waitForPossibleCall(getClass());

            Map<String, String> params = new HashMap<String, String>();
            String request = getExchange().Request("deposit_address", params);

            JSONObject obj = new JSONObject(request);

            return obj.getString(cryptoCurrency);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exmo exchange (getBalance) failed with message: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String sellCoins(BigDecimal cryptoAmount, String cryptoCurrency, String fiatCurrencyToUse,
            String description) {

        int orderId = 0;
        String currencyPair = cryptoCurrency + "_" + fiatCurrencyToUse;
        JSONObject order = null;
        boolean result_order = false;

        log.info("Calling Exmo exchange (SELL " + cryptoAmount + " " + cryptoCurrency + ")");

        try {

            try {

                DDOSUtils.waitForPossibleCall(getClass());

                BigDecimal calculatedBuyPrice = calculateSellPrice(cryptoCurrency, fiatCurrencyToUse, cryptoAmount);

                BigDecimal fiatBalance = getFiatBalance(fiatCurrencyToUse);

                if (fiatBalance.compareTo(calculatedBuyPrice) < 0) {
                    System.out.print("Saldo insuficiente: ");
                    System.out.print("balance : " + fiatBalance);
                    System.out.print("valor necessário: " + calculatedBuyPrice);
                    // return null;
                }

                order = openOrder(currencyPair, cryptoAmount, "market_sell");

                if (order == null) {
                    System.out.println("Não encontramos o objeto result");
                    log.error("Não encontramos o objeto result");
                    return null;
                }

                if (!order.has("result") || order.isNull("result")) {
                    System.out.println("Não encontramos o objeto result");
                    log.error("Não encontramos o objeto result");
                    return null;
                }

                result_order = order.getBoolean("result");

                if (!result_order) {
                    System.out.println("O object result está falso");
                    log.error("O object result está falso");
                    return null;
                }

                if (!order.has("order_id") || order.isNull("order_id")) {
                    System.out.println("Não encontramos o objeto order_id");
                    log.error("Não encontramos o objeto order_id");
                    return null;
                }

                if (order.has("error") && !order.isNull("error") && !order.getString("error").isEmpty()) {
                    System.out.println("ERROR: " + order.getString("error"));
                    log.error("ERROR: " + order.getString("error"));
                    return null;
                }

                orderId = order.getInt("order_id");

                log.debug("orderId = " + orderId + " " + order);

                log.debug("sell cons - order_create ");

                // log.debug("marketOrder = " + order.);

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(2000); // give exchange 2 seconds to reflect open order in order book
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // get open orders log.debug("Open orders:");

            boolean orderProcessed = false;
            int numberOfChecks = 0;

            while (!orderProcessed && numberOfChecks < 10) {

                boolean orderFound = false;

                JSONArray openOrders = getOpenOrders(currencyPair);

                if (openOrders == null) {
                    orderProcessed = true;
                } else {

                    DDOSUtils.waitForPossibleCall(getClass());

                    for (int i = 0; i < openOrders.length(); i++) {

                        JSONObject openOrder = openOrders.getJSONObject(i);

                        log.debug("openOrder = " + openOrder);

                        if (orderId == (openOrder.getInt("order_id"))) {
                            orderFound = true;
                            break;
                        }
                    }
                }

                if (orderFound) {
                    log.debug("Waiting for order to be processed.");
                    try {
                        Thread.sleep(3000);

                        // don't get your ip address banned

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    orderProcessed = true;
                }
                numberOfChecks++;
            }
            if (orderProcessed) {
                return String.valueOf(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exmo exchange (sellCoins) failed with message: " + e.getMessage());
        }
        return null;
    }

    @Override
    public ITask createSellCoinsTask(BigDecimal amount, String cryptoCurrency, String fiatCurrencyToUse,
            String description) {
        CurrencyPair currencyPair = new CurrencyPair(cryptoCurrency, fiatCurrencyToUse);
        if (!isCurrencyPairSupported(currencyPair)) {
            return null;
        }
        return new SellCoinsTask(amount, cryptoCurrency, fiatCurrencyToUse, description);
    }

    class PurchaseCoinsTask implements ITask {
        private long MAXIMUM_TIME_TO_WAIT_FOR_ORDER_TO_FINISH = 5 * 60 * 60 * 1000; // 5 hours

        private BigDecimal amount;
        private String cryptoCurrency;
        private String fiatCurrencyToUse;
        private String description;

        private String orderId;
        private String result;
        private boolean finished;

        PurchaseCoinsTask(BigDecimal amount, String cryptoCurrency, String fiatCurrencyToUse, String description) {

            this.amount = amount;
            this.cryptoCurrency = cryptoCurrency;
            this.fiatCurrencyToUse = fiatCurrencyToUse;
            this.description = description;
        }

        @Override
        public boolean onCreate() {

            orderId = purchaseCoins(amount, cryptoCurrency, fiatCurrencyToUse, description);

            return (orderId != null && !orderId.isEmpty() && orderId.length() > 0);

        }

        @Override
        public boolean onDoStep() {

            if (orderId == null) {
                log.debug("Giving up on waiting for trade to complete. Because it did not happen");
                finished = true;
                result = "Skipped";
                return false;
            }

            // get open orders
            boolean orderProcessed = false;
            long checkTillTime = System.currentTimeMillis() + MAXIMUM_TIME_TO_WAIT_FOR_ORDER_TO_FINISH;
            if (System.currentTimeMillis() > checkTillTime) {
                log.debug("Giving up on waiting for trade " + orderId + " to complete");
                finished = true;
                return false;
            }

            log.debug("Open orders:");
            boolean orderFound = isOrderStillOpen(orderId, cryptoCurrency + "_" + fiatCurrencyToUse);

            if (orderFound) {
                log.debug("Waiting for order to be processed.");
            } else {
                orderProcessed = true;
            }

            if (orderProcessed) {
                result = orderId;
                finished = true;
            }

            return result != null;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public String getResult() {
            return result;
        }

        @Override
        public boolean isFailed() {
            return finished && result == null;
        }

        @Override
        public void onFinish() {
            log.debug("Purchase task finished.");
        }

        @Override
        public long getShortestTimeForNexStepInvocation() {
            return 5 * 1000; // it doesn't make sense to run step sooner than after 5 seconds
        }
    }

    class SellCoinsTask implements ITask {
        private long MAXIMUM_TIME_TO_WAIT_FOR_ORDER_TO_FINISH = 5 * 60 * 60 * 1000; // 5 hours

        private BigDecimal cryptoAmount;
        private String cryptoCurrency;
        private String fiatCurrencyToUse;
        private String description;

        private String orderId;
        private String result;
        private boolean finished;

        SellCoinsTask(BigDecimal cryptoAmount, String cryptoCurrency, String fiatCurrencyToUse, String description) {
            this.cryptoAmount = cryptoAmount;
            this.cryptoCurrency = cryptoCurrency;
            this.fiatCurrencyToUse = fiatCurrencyToUse;
            this.description = description;

        }

        @Override
        public boolean onCreate() {

            log.debug("SELL TASK - onCreate - SMARTCASH ORDERID" + orderId);

            orderId = sellCoins(cryptoAmount, cryptoCurrency, fiatCurrencyToUse, description);

            boolean hasOrder = (orderId != null && !orderId.isEmpty() && orderId.length() > 0);

            return hasOrder;
        }

        @Override
        public boolean onDoStep() {

            if (orderId == null) {
                log.debug("Giving up on waiting for trade to complete. Because it did not happen");
                finished = true;
                result = "Skipped";
                return false;
            }

            // get open orders
            boolean orderProcessed = false;
            long checkTillTime = System.currentTimeMillis() + MAXIMUM_TIME_TO_WAIT_FOR_ORDER_TO_FINISH;
            if (System.currentTimeMillis() > checkTillTime) {
                log.debug("Giving up on waiting for trade " + orderId + " to complete");
                finished = true;
                return false;
            }

            log.debug("Open orders:");
            boolean orderFound = isOrderStillOpen(orderId, cryptoCurrency + "_" + fiatCurrencyToUse);

            if (orderFound) {
                log.debug("Waiting for order to be processed.");
            } else {
                orderProcessed = true;
            }

            if (orderProcessed) {
                result = orderId;
                finished = true;
            }

            return result != null;
        }

        @Override
        public boolean isFinished() {
            log.info("SELL TASK - isFinished - SMARTCASH ");
            return finished;
        }

        @Override
        public String getResult() {

            log.info("SELL TASK - getResult - SMARTCASH ");
            return result;
        }

        @Override
        public boolean isFailed() {

            log.info("SELL TASK - isFaile - SMARTCASH ");
            return finished && result == null;
        }

        @Override
        public void onFinish() {

            log.info("SELL TASK - onFinish - SMARTCASH ");

            log.debug("Sell task finished.");
        }

        @Override
        public long getShortestTimeForNexStepInvocation() {
            return 5 * 1000; // it doesn't make sense to run step sooner than after 5 seconds
        }
    }

    private BigDecimal getMeasureCryptoAmount() {
        return new BigDecimal(5);
    }

    @Override
    public BigDecimal getExchangeRateForBuy(String cryptoCurrency, String fiatCurrency) {
        BigDecimal result = calculateBuyPrice(cryptoCurrency, fiatCurrency, getMeasureCryptoAmount());
        if (result != null) {
            return result.divide(getMeasureCryptoAmount(), 2, BigDecimal.ROUND_UP);
        }
        return null;
    }

    @Override
    public BigDecimal getExchangeRateForSell(String cryptoCurrency, String fiatCurrency) {

        log.debug("GET Exchange SELL Rate : " + cryptoCurrency + fiatCurrency);

        BigDecimal rate = null;

        BigDecimal result = calculateSellPrice(cryptoCurrency, fiatCurrency, getMeasureCryptoAmount());
        if (result != null) {
            rate = result.divide(getMeasureCryptoAmount(), 2, BigDecimal.ROUND_DOWN);
        }
        log.debug("GET Exchange SELL Rate : " + cryptoCurrency + fiatCurrency + " ===> " + rate);

        return rate;
    }

    private JSONObject getOpenOrderBooks(String currencyPair) {

        HashMap<String, String> paramsOpenOrders = new HashMap<String, String>();
        paramsOpenOrders.put("pair", currencyPair);
        paramsOpenOrders.put("limit", "100");

        String request = getExchange().Request("order_book", paramsOpenOrders);

        JSONObject orders = new JSONObject(request);

        if (!orders.has(currencyPair) || orders.isNull(currencyPair))
            return null;
        else
            return orders.getJSONObject(currencyPair);

    }

    private class Ask {
        BigDecimal price, quantity, amount;

        public Ask(BigDecimal price, BigDecimal quantity, BigDecimal amount) {
            this.price = price;
            this.quantity = quantity;
            this.amount = amount;
        }

        public String toString() {
            return this.price + " " + this.quantity + " " + this.amount;
        }

    }

    class SortAskByPrice implements Comparator<Ask> {
        // Used for sorting in ascending order of
        // roll number
        public int compare(Ask a, Ask b) {

            return a.price.compareTo(b.price);

        }
    }

    @Override
    public BigDecimal calculateBuyPrice(String cryptoCurrency, String fiatCurrency, BigDecimal cryptoAmount) {

        String currencyPair = cryptoCurrency + "_" + fiatCurrency;

        BigDecimal totalFiatPrice = BigDecimal.ZERO;

        BigDecimal totalCryptoInOrder = BigDecimal.ZERO;

        DDOSUtils.waitForPossibleCall(getClass());

        JSONArray openOrders = getOpenOrderBooks(currencyPair).getJSONArray("bid");

        Ask[] allAsk = new Ask[openOrders.length()];

        for (int i = 0; i < openOrders.length(); i++) {

            JSONArray ask = openOrders.getJSONArray(i);

            if (ask.length() == 3) {
                allAsk[i] = new Ask(ask.getBigDecimal(0), ask.getBigDecimal(1), ask.getBigDecimal(2));
            }
        }

        Arrays.sort(allAsk, new SortAskByPrice());

        for (int i = 0; i < allAsk.length; i++) {
            Ask ask = allAsk[i];

            // System.out.println(ask.toString());

            if (totalCryptoInOrder.compareTo(cryptoAmount) == -1) {
                totalCryptoInOrder = totalCryptoInOrder.add(ask.quantity);
                totalFiatPrice = totalFiatPrice.add(ask.price);
            }
        }

        // System.out.println("totalFiatPrice = " + totalFiatPrice);

        return totalFiatPrice;
    }

    @Override
    public BigDecimal calculateSellPrice(String cryptoCurrency, String fiatCurrency, BigDecimal cryptoAmount) {

        log.debug("Calculate SELL PRICE : " + cryptoCurrency + fiatCurrency);

        String currencyPair = cryptoCurrency + "_" + fiatCurrency;

        BigDecimal totalFiatPrice = BigDecimal.ZERO;

        BigDecimal totalCryptoInOrder = BigDecimal.ZERO;

        DDOSUtils.waitForPossibleCall(getClass());

        JSONArray openOrders = getOpenOrderBooks(currencyPair).getJSONArray("bid");

        Ask[] allAsk = new Ask[openOrders.length()];

        for (int i = 0; i < openOrders.length(); i++) {

            JSONArray ask = openOrders.getJSONArray(i);

            if (ask.length() == 3) {
                allAsk[i] = new Ask(ask.getBigDecimal(0), ask.getBigDecimal(1), ask.getBigDecimal(2));
            }
        }

        Arrays.sort(allAsk, Collections.reverseOrder(new SortAskByPrice()));

        for (int i = 0; i < allAsk.length; i++) {
            Ask ask = allAsk[i];

            // System.out.println(ask.toString());

            if (totalCryptoInOrder.compareTo(cryptoAmount) == -1) {
                totalCryptoInOrder = totalCryptoInOrder.add(ask.quantity);
                totalFiatPrice = totalFiatPrice.add(ask.price);
            }
        }

        // System.out.println("totalFiatPrice to sell = " + totalFiatPrice);

        return totalFiatPrice;

    }
}
