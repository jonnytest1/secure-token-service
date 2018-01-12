package de.adorsys.sts.persistence;

import de.adorsys.sts.keymanagement.model.StsKeyStore;
import de.adorsys.sts.keymanagement.service.KeyManagementProperties;
import de.adorsys.sts.keymanagement.persistence.KeyStoreRepository;
import org.adorsys.encobject.domain.ObjectHandle;
import org.adorsys.encobject.filesystem.FsPersistenceFactory;
import org.adorsys.encobject.service.*;
import org.adorsys.jkeygen.pwd.PasswordCallbackHandler;

import javax.annotation.PostConstruct;
import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class FsPersistenceKeyStoreRepository implements KeyStoreRepository {

    private final FsPersistenceFactory persFactory;
    private final String keystoreContainerName;
    private final String keystoreName;
    private final CallbackHandler keyPassHandler;

    public FsPersistenceKeyStoreRepository(FsPersistenceFactory persFactory, String keystoreContainerName, String keystoreName, String keyStorePassword) {
        this.persFactory = persFactory;
        this.keystoreContainerName = keystoreContainerName;
        this.keystoreName = keystoreName;

        keyPassHandler = new PasswordCallbackHandler(keyStorePassword.toCharArray());
    }

    public FsPersistenceKeyStoreRepository(
            FsPersistenceFactory persFactory,
            KeyManagementProperties keyManagementProperties
    ) {
        this.persFactory = persFactory;
        this.keystoreContainerName = keyManagementProperties.getPersistence().getContainerName();
        this.keystoreName = keyManagementProperties.getKeystore().getName();

        String keyStorePassword = keyManagementProperties.getPersistence().getPassword();
        keyPassHandler = new PasswordCallbackHandler(keyStorePassword.toCharArray());
    }

    @PostConstruct
    public void postConstruct() {
        ContainerPersistence containerPersistence = persFactory.getContainerPersistence();
        if (!containerPersistence.containerExists(keystoreContainerName)) {
            try {
                containerPersistence.creteContainer(keystoreContainerName);
            } catch (ContainerExistsException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public StsKeyStore load() {
        ObjectHandle handle = new ObjectHandle(keystoreContainerName, keystoreName);

        try {
            KeyStore keyStore = persFactory.getKeystorePersistence().loadKeystore(handle, keyPassHandler);

            return StsKeyStore.builder()
                    .keyStore(keyStore)
                    // TODO load key attributes
                    .keyEntries(null)
                    .build();
        } catch (KeystoreNotFoundException | CertificateException | WrongKeystoreCredentialException | MissingKeystoreAlgorithmException | MissingKeystoreProviderException | MissingKeyAlgorithmException | IOException | UnknownContainerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists() {
        ObjectHandle handle = new ObjectHandle(keystoreContainerName, keystoreName);

        return persFactory.getKeystorePersistence().hasKeystore(handle);
    }

    @Override
    public void save(StsKeyStore keyStore) {
        ObjectHandle handle = new ObjectHandle(keystoreContainerName, keystoreName);

        try {
            // TODO save key attributes
            persFactory.getKeystorePersistence().saveKeyStore(keyStore.getKeyStore(), keyPassHandler, handle);
        } catch (NoSuchAlgorithmException | CertificateException | UnknownContainerException e) {
            throw new RuntimeException(e);
        }
    }
}
