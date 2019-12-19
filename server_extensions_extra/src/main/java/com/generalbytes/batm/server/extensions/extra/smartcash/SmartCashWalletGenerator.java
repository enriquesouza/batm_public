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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.generalbytes.batm.server.extensions.IExtensionContext;
import com.generalbytes.batm.server.extensions.IPaperWallet;
import com.generalbytes.batm.server.extensions.IPaperWalletGenerator;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SmartCashWalletGenerator implements IPaperWalletGenerator {

    private static final String MSG_TO_REPLACE = "New wallet %s use your onetime password to open the attachment.";
    private static final String BATM_CONFIG_TEMPLATE_WALLET_S_TXT = "/batm/config/template_wallet_%s.txt";
    private static final String UTF_8 = "UTF-8";
    private static final String APPLICATION_ZIP = "application/zip";
    private static final String ZIP = "zip";
    private static final String BATM_CONFIG_TEMPLATE_WALLET_EN_TXT = "/batm/config/template_wallet_en.txt";
    private static final String ADDRESS2 = "address";
    private static final String PRIVATE_KEY = "privateKey";
    private static final String HTTP_LOCALHOST_3000_GENERATEWALLET = "http://localhost:3000/generatewallet";
    private static final Logger log = LoggerFactory.getLogger("batm.master.SmartCashWalletGenerator");
    private IExtensionContext ctx;

    public SmartCashWalletGenerator(String prefix, IExtensionContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public IPaperWallet generateWallet(String cryptoCurrency, String oneTimePassword, String userLanguage) {

        String strResponse = null;
        String privateKey = null;
        String address = null;
        Request request = null;
        Response response = null;
        OkHttpClient client = null;
        byte[] content = null;
        String messageText = null;
        String messageTextLang = null;
        String messageTextEN = null;
        String URL = HTTP_LOCALHOST_3000_GENERATEWALLET;
        JSONObject wallet = null;

        try {

            client = new OkHttpClient();
            request = new Request.Builder().url(URL).build();
            response = client.newCall(request).execute();
            strResponse = response.body().string();

            if (strResponse != null && !strResponse.isEmpty()) {

                wallet = new JSONObject(strResponse);

                if (!wallet.isNull(PRIVATE_KEY)) {
                    privateKey = wallet.getString(PRIVATE_KEY);
                }
                if (!wallet.isNull(ADDRESS2)) {
                    address = wallet.getString(ADDRESS2);
                }

                content = ctx.createPaperWallet7ZIP(privateKey, address, oneTimePassword, cryptoCurrency);

                log.info("RESPONSE do NODE: " + strResponse);

                // send wallet to customer

                messageText = String.format(MSG_TO_REPLACE, address);

                messageTextLang = readTemplate(String.format(BATM_CONFIG_TEMPLATE_WALLET_S_TXT, userLanguage));

                if (messageTextLang != null) {
                    messageText = messageTextLang;
                } else {
                    messageTextEN = readTemplate(BATM_CONFIG_TEMPLATE_WALLET_EN_TXT);
                    if (messageTextEN != null) {
                        messageText = messageTextEN;
                    }
                }

                return new SmartCashPaperWallet(cryptoCurrency, content, address, privateKey, messageText,
                        APPLICATION_ZIP, ZIP);
            }

        } catch (IOException e) {

            System.err.println("Request fail: " + e.toString());
            log.error("ERRO PAPER WALLET" + e.getMessage());

        }

        return null;
    }

    @SuppressWarnings("all")
    private String readTemplate(String templateFile) {
        File f = new File(templateFile);
        if (f.exists() && f.canRead()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(templateFile)), UTF_8);
                return content;
            } catch (IOException e) {
                log.error("readTemplate", e);
            }
        }
        return null;
    }

}
