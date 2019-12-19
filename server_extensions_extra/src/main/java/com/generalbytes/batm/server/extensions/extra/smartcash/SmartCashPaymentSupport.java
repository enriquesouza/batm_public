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
import com.generalbytes.batm.server.extensions.ICryptoAddressValidator;
import com.generalbytes.batm.server.extensions.extra.bitcoincash.test.PRS;
import com.generalbytes.batm.server.extensions.extra.common.AbstractRPCPaymentSupport;
import com.generalbytes.batm.server.extensions.extra.common.RPCClient;
import com.generalbytes.batm.server.extensions.payment.IPaymentRequestListener;
import com.generalbytes.batm.server.extensions.payment.PaymentRequest;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class SmartCashPaymentSupport extends AbstractRPCPaymentSupport {

    private SmartCashAddressValidator addressValidator = new SmartCashAddressValidator();

    private static final long MAXIMUM_WAIT_FOR_POSSIBLE_REFUND_MILLIS = TimeUnit.DAYS.toMillis(3); // 3 days
    private static final long MAXIMUM_WATCHING_TIME_MILLIS = TimeUnit.DAYS.toMillis(3); // 3 days (exactly plus Sell
    // Offer Expiration 5-120
    // minutes)
    private static final BigDecimal TOLERANCE = new BigDecimal("100"); // Received amount should be cryptoTotalToSend
    // +- tolerance

    @Override
    public String getCurrency() {
        return CryptoCurrency.SMART.getCode();
    }

    @Override
    public long getMaximumWatchingTimeMillis() {
        return MAXIMUM_WATCHING_TIME_MILLIS;
    }

    @Override
    public long getMaximumWaitForPossibleRefundInMillis() {
        return MAXIMUM_WAIT_FOR_POSSIBLE_REFUND_MILLIS;
    }

    @Override
    public BigDecimal getTolerance() {
        return TOLERANCE;
    }

    @Override
    public BigDecimal getMinimumNetworkFee(RPCClient client) {
        return client.getNetworkInfo().relayFee();
    }

    @Override
    public ICryptoAddressValidator getAddressValidator() {
        return addressValidator;
    }

    @Override
    public int calculateTransactionSize(int numberOfInputs, int numberOfOutputs) {
        return ((numberOfInputs * 148) + (numberOfOutputs * 34) + 19) / 1024;
    }

    @Override
    public BigDecimal calculateTxFee(int numberOfInputs, int numberOfOutputs, RPCClient client) {
        double fee = (((numberOfInputs * 148) + (numberOfOutputs * 34) + 19) / 1024D) * 0.001D;
        return new BigDecimal(fee);
    }

    public static void main(String[] args) {
        // You need to have node running: i.e.: bitcoind -rpcuser=rpcuser
        // -rpcpassword=rpcpassword -rpcport=8332


        final String URL_RPC = "http://enrique:eusouoricao@66.172.12.175:9679";
        final String EXCHANGE_PARAMS = "exmo:K-a09eb2a5ade5bf4c284ab41bb2a5790799178a41:S-074e037dc012db6d1975a1d2478e93596a43331d:EUR";
        final String WALLET_LOGIN = "http:enrique:eusouoricao:66.172.12.175:9679";
        final String RATE_SOURCE = "smartapi:EUR";


        SmartCashRPCWallet wallet = new SmartCashRPCWallet(URL_RPC, "");
        SmartCashPaymentSupport ps = new SmartCashPaymentSupport();
        ps.init(null);
        PRS spec = new PRS(ps.getCurrency(), "Just a test", 60 * 15, // 15 min
            3, false, false, new BigDecimal("100"), new BigDecimal("100"), wallet);
        spec.addOutput("SgPMhNeG16Ty6VaPSnAtxNJAQ2JRnhTGaQ", new BigDecimal("0.001"));

        PaymentRequest pr = ps.createPaymentRequest(spec);
        System.out.println(pr);
        pr.setListener(new IPaymentRequestListener() {
            @Override
            public void stateChanged(PaymentRequest request, int previousState, int newState) {
                System.out.println(
                    "stateChanged = " + request + " previousState: " + previousState + " newState: " + newState);
            }

            @Override
            public void numberOfConfirmationsChanged(PaymentRequest request, int numberOfConfirmations,
                                                     Direction direction) {
                System.out.println("numberOfConfirmationsChanged = " + request + " numberOfConfirmations: "
                    + numberOfConfirmations + " direction: " + direction);
            }

            @Override
            public void refundSent(PaymentRequest request, String toAddress, String cryptoCurrency, BigDecimal amount) {
                System.out.println("refundSent = " + request + " toAddress: " + toAddress + " cryptoCurrency: "
                    + cryptoCurrency + " " + amount);
            }
        });
        System.out.println("Waiting for transfer");
        try {
            Thread.sleep(20 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
