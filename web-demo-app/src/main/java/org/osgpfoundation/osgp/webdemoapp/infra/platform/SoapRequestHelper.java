/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgpfoundation.osgp.webdemoapp.infra.platform;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

/**
 * Helper class to create WebServiceTemplates for each specific domain.
 *
 */
public class SoapRequestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoapRequestHelper.class);

    private Jaxb2Marshaller marshaller;
    private KeyStoreHelper keyStoreHelper;

    private SaajSoapMessageFactory messageFactory;

    public SoapRequestHelper(final SaajSoapMessageFactory messageFactory, final KeyStoreHelper keyStoreHelper) {
        this.messageFactory = messageFactory;
        this.keyStoreHelper = keyStoreHelper;
    }

    /**
     * Helper function to create a web service template to handle soap requests
     * for the Admin domain
     *
     * @return WebServiceTemplate
     */
    public WebServiceTemplate createAdminRequest() {
        this.initMarshaller("com.alliander.osgp.adapter.ws.schema.admin.devicemanagement");

        final String uri = "https://localhost/osgp-adapter-ws-admin/admin/deviceManagementService/DeviceManagement";

        final WebServiceTemplate webServiceTemplate = new WebServiceTemplate(this.messageFactory);

        webServiceTemplate.setDefaultUri(uri);
        webServiceTemplate.setMarshaller(this.marshaller);
        webServiceTemplate.setUnmarshaller(this.marshaller);

        webServiceTemplate.setCheckConnectionForFault(true);

        webServiceTemplate.setInterceptors(new ClientInterceptor[] { this
                .createClientInterceptor("http://www.alliander.com/schemas/osp/common") });

        webServiceTemplate.setMessageSender(this.createHttpMessageSender());

        return webServiceTemplate;
    }

    /**
     * Helper function to create a web service template to handle soap requests
     * for the Public Lighting domain
     *
     * @return WebServiceTemplate
     */
    public WebServiceTemplate createPublicLightingRequest() {
        this.initMarshaller("com.alliander.osgp.adapter.ws.schema.publiclighting.adhocmanagement");

        final String uri = "https://localhost/osgp-adapter-ws-publiclighting/publiclighting/adHocManagementService/AdHocManagement";

        final WebServiceTemplate webServiceTemplate = new WebServiceTemplate(this.messageFactory);

        webServiceTemplate.setDefaultUri(uri);
        webServiceTemplate.setMarshaller(this.marshaller);
        webServiceTemplate.setUnmarshaller(this.marshaller);

        webServiceTemplate.setCheckConnectionForFault(true);

        webServiceTemplate.setInterceptors(new ClientInterceptor[] { this
                .createClientInterceptor("http://www.alliander.com/schemas/osgp/common") });

        webServiceTemplate.setMessageSender(this.createHttpMessageSender());

        return webServiceTemplate;
    }

    /**
     * Initializes the JaxB Marshaller
     *
     * @param marshallerContext
     */
    private void initMarshaller(final String marshallerContext) {
        this.marshaller = new Jaxb2Marshaller();

        this.marshaller.setContextPath(marshallerContext);
    }

    /**
     * Creates a HttpComponentsMessageSender for communication with the
     * platform.
     *
     * @return HttpComponentsMessageSender
     */
    private HttpComponentsMessageSender createHttpMessageSender() {

        final HttpComponentsMessageSender sender = new HttpComponentsMessageSender();

        final HttpClientBuilder builder = HttpClients.custom();
        builder.addInterceptorFirst(new ContentLengthHeaderRemoveInterceptor());
        try {
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadKeyMaterial(this.keyStoreHelper.getKeyStore(), this.keyStoreHelper.getKeyStorePwAsChar())
                    .loadTrustMaterial(this.keyStoreHelper.getTrustStore()).build();
            final SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext);
            builder.setSSLSocketFactory(sslConnectionFactory);
            sender.setHttpClient(builder.build());
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Unbale to cretae SSL context", e);
        }

        return sender;
    }

    /**
     * Create a ClientIntercepter, used for the WebServiceTemplate.
     *
     * @param namespace
     * @return ClientInterceptor
     */
    private ClientInterceptor createClientInterceptor(final String namespace) {
        return new IdentificationClientInterceptor("test-org", "demo-app-user", "demo-app", namespace,
                "OrganisationIdentification", "UserName", "ApplicationName");
    }

}
