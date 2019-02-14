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

import com.generalbytes.batm.server.extensions.Currencies;
import com.generalbytes.batm.server.extensions.ICryptoAddressValidator;
import com.generalbytes.batm.server.extensions.IWallet;
import com.generalbytes.batm.server.extensions.extra.bitcoincash.test.PRS;
import com.generalbytes.batm.server.extensions.extra.common.AbstractRPCPaymentSupport;
import com.generalbytes.batm.server.extensions.payment.IPaymentRequestListener;
import com.generalbytes.batm.server.extensions.payment.IPaymentRequestSpecification;
import com.generalbytes.batm.server.extensions.payment.PaymentRequest;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class SmartCashPaymentSupport extends AbstractRPCPaymentSupport {

    private static final Logger log = LoggerFactory.getLogger("batm.master.RPCPaymentSupport");

    private SmartCashAddressValidator addressValidator = new SmartCashAddressValidator();

    private static final long MAXIMUM_WAIT_FOR_POSSIBLE_REFUND_MILLIS = TimeUnit.DAYS.toMillis(3); // 3 days
    private static final long MAXIMUM_WATCHING_TIME_MILLIS = TimeUnit.DAYS.toMillis(3); // 3 days (exactly plus Sell
                                                                                        // Offer Expiration 5-120
                                                                                        // minutes)
    private static final BigDecimal TOLERANCE = new BigDecimal("1"); // Received amount should be cryptoTotalToSend +-
                                                                     // tolerance
    private static final BigDecimal MINIMUM_NETWORK_FEE = new BigDecimal("0.001");

    @Override
    public String getCurrency() {
        return Currencies.SMART;
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
    public ICryptoAddressValidator getAddressValidator() {
        return addressValidator;
    }

    @Override
    public PaymentRequest createPaymentRequest(IPaymentRequestSpecification spec) {
        try {

            log.debug("CREATING A PAYMENT REQUEST");

            IWallet wallet = getWallet();

            //spec = getPaymentRequestSpecification();

            long validTill = System.currentTimeMillis() + (spec.getValidInSeconds() * 1000);


            log.debug("Valid till " + validTill);


            String paymentAddress = null;

            BigDecimal cryptoTotalToSend = spec.getTotal();

            if(spec == null)
            {
                log.error("The SPEC is NULL");
                return null;
            }

            if (spec.isDoNotForward() && spec.getOutputs().size() == 1) { // Sometimes it is inefficient to forward
                                                                          // transaction when only one output is defined
                paymentAddress = spec.getOutputs().get(0).getAddress();
                // no need to modify cryptoTotalToSend value as we will not be forwarding coins
                // we don't need extra money for forwarding
            } else {
                paymentAddress = RPCClient.cleanAddressFromPossiblePrefix(getClient(wallet).getNewAddress());

                log.debug("paymentAddress " + paymentAddress);

                // add additional fee



                int outputs = spec.getOutputs().size();


                BigDecimal feeCalculated = calculateTxFee(1, outputs, getClient(wallet));
                BigDecimal optimalMiningFee = spec.getOptimalMiningFee(feeCalculated,
                        calculateTransactionSize(1, outputs));

                if (spec.isZeroFixedFee()) {
                    cryptoTotalToSend = cryptoTotalToSend.add(optimalMiningFee);
                } else {
                    // correct outputs: remove mining fee from expected total amount that will be
                    // I/O difference for creating mining fee
                    spec.removeTotalAmountFromOutputs(optimalMiningFee);
                }
            }

            cryptoTotalToSend = cryptoTotalToSend.setScale(6, BigDecimal.ROUND_HALF_UP); // round to 6 decimal places

            log.debug("CRYPTO TO SEND " + cryptoTotalToSend);

            log.debug("CRYPTO: " + spec.getCryptoCurrency());
            log.debug("DESCRIPTON: " + spec.getDescription());
            log.debug("TOLERANCE: " + getTolerance());
            log.debug("getRemoveAfterNumberOfConfirmationsOfIncomingTransaction: " + spec.getRemoveAfterNumberOfConfirmationsOfIncomingTransaction());
            log.debug("getRemoveAfterNumberOfConfirmationsOfOutgoingTransaction: " + spec.getRemoveAfterNumberOfConfirmationsOfOutgoingTransaction());


            final PaymentRequest paymentRequest = new PaymentRequest(spec.getCryptoCurrency(), spec.getDescription(),
                    validTill, paymentAddress, cryptoTotalToSend, getTolerance(),
                    spec.getRemoveAfterNumberOfConfirmationsOfIncomingTransaction(),
                    spec.getRemoveAfterNumberOfConfirmationsOfOutgoingTransaction(), (IWallet)wallet);

            PaymentTracker paymentTracker = new PaymentTracker(!spec.isDoNotForward(), paymentRequest, spec);
            requests.put(paymentRequest, paymentTracker);
            startWatchingAddress(getClient(wallet), getCurrency(), paymentAddress, paymentTracker, paymentRequest); // start
                                                                                                                    // watching
                                                                                                                    // the
                                                                                                                    // address
            return paymentRequest;
        } catch (BitcoinRPCException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        // You need to have node running: i.e.: bitcoind -rpcuser=rpcuser
        // -rpcpassword=rpcpassword -rpcport=8332

        SmartCashRPCWallet wallet = new SmartCashRPCWallet(SmartCashConstants.URL_RPC, "");
        SmartCashPaymentSupport ps = new SmartCashPaymentSupport();
        ps.init(null);
        PRS spec = new PRS(ps.getCurrency(), "Just a test", 60 * 15, // 15 min
                3, false, false, new BigDecimal("100"), new BigDecimal("100"), wallet);
        spec.addOutput("SYPoEuzFQCnP1YnY5mnKJFv9P46txNBBSG", new BigDecimal("0.001"));

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

    public IWallet getWallet() {
        //TODO: Remove all fixed parameters; This node is for test!
        return new SmartCashRPCWallet(SmartCashConstants.URL_RPC, "");
    }

    /*
     * public int calculateTransactionSize(int numberOfInputs, int numberOfOutputs)
     * { return (numberOfInputs * 149) + (numberOfOutputs * 34) + 10; }
     */

    BigDecimal calculateTxFee(int numberOfInputs, int numberOfOutputs, RPCClient client) {
        final int transactionSize = calculateTransactionSize(numberOfInputs, numberOfOutputs);
        try {
            return new BigDecimal(client.getEstimateFee(1)).multiply(new BigDecimal(transactionSize));
        } catch (BitcoinRPCException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BigDecimal getMinimumNetworkFee(com.generalbytes.batm.server.extensions.extra.common.RPCClient client) {
        return MINIMUM_NETWORK_FEE;
    }

    @Override
    protected int calculateTransactionSize(int numberOfInputs, int numberOfOutputs) {
        return 0;
    }

    @Override
    protected BigDecimal calculateTxFee(int numberOfInputs, int numberOfOutputs,
            com.generalbytes.batm.server.extensions.extra.common.RPCClient client) {
        return MINIMUM_NETWORK_FEE;
    }

}
