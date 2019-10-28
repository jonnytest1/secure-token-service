package de.adorsys.sts.persistence.jpa.mapping;

import com.googlecode.cqengine.query.QueryFactory;
import de.adorsys.keymanagement.api.keystore.KeyStoreView;
import de.adorsys.keymanagement.juggler.services.Juggler;
import de.adorsys.sts.keymanagement.model.KeyEntry;
import de.adorsys.sts.keymanagement.model.PasswordCallbackHandler;
import de.adorsys.sts.keymanagement.model.StsKeyEntry;
import de.adorsys.sts.keymanagement.model.StsKeyStore;
import de.adorsys.sts.keymanagement.service.KeyManagementProperties;
import de.adorsys.sts.persistence.jpa.entity.JpaKeyEntryAttributes;
import de.adorsys.sts.persistence.jpa.entity.JpaKeyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.adorsys.keymanagement.core.view.EntryViewImpl.A_ID;

@Component
public class KeyStoreEntityMapper {

    private final Juggler juggler;
    private final PasswordCallbackHandler keyPassHandler;
    private final String keystoreName;

    @Autowired
    public KeyStoreEntityMapper(
            Juggler juggler,
            KeyManagementProperties keyManagementProperties
    ) {
        this.juggler = juggler;
        String keyStorePassword = keyManagementProperties.getKeystore().getPassword();
        keyPassHandler = new PasswordCallbackHandler(keyStorePassword.toCharArray());
        keystoreName = keyManagementProperties.getKeystore().getName();
    }

    public JpaKeyStore mapToEntity(StsKeyStore keyStore) {
        JpaKeyStore persistentKeyStore = new JpaKeyStore();

        mapIntoEntity(keyStore, persistentKeyStore);

        return persistentKeyStore;
    }

    public void mapIntoEntity(StsKeyStore keyStore, JpaKeyStore persistentKeyStore) {
        byte[] bytes = juggler.serializeDeserialize().serialize(keyStore.getKeyStore(), keyPassHandler::getPassword);

        persistentKeyStore.setName(keystoreName);
        persistentKeyStore.setKeystore(bytes);
        persistentKeyStore.setType(keyStore.getKeyStore().getType());
        persistentKeyStore.setLastUpdate(keyStore.getLastUpdate());
    }

    public StsKeyStore mapFromEntity(JpaKeyStore persistentKeyStore, List<JpaKeyEntryAttributes> persistentKeyEntries) {
        KeyStore keyStore = juggler.serializeDeserialize()
                .deserialize(persistentKeyStore.getKeystore(), keyPassHandler::getPassword);

        Map<String, StsKeyEntry> mappedKeyEntries = mapFromEntities(keyStore, persistentKeyEntries);

        return StsKeyStore.builder()
                .keyStore(keyStore)
                .keyEntries(mappedKeyEntries)
                .lastUpdate(persistentKeyStore.getLastUpdate())
                .build();
    }

    private Map<String, StsKeyEntry> mapFromEntities(KeyStore keyStore, List<JpaKeyEntryAttributes> persistentKeyEntries) {
        Map<String, StsKeyEntry> mappedKeyEntries = new HashMap<>();
        KeyStoreView view = juggler.readKeys().fromKeyStore(keyStore, id -> keyPassHandler.getPassword());

        for (JpaKeyEntryAttributes keyEntryAttributes : persistentKeyEntries) {
            KeyStore.Entry keyEntry = view.entries()
                    .retrieve(QueryFactory.equal(A_ID, keyEntryAttributes.getAlias()))
                    .toCollection().first()
                    .getEntry();

            StsKeyEntry mappedKeyEntry = mapFromEntity(keyEntry, keyEntryAttributes);
            mappedKeyEntries.put(mappedKeyEntry.getAlias(), mappedKeyEntry);
        }

        return mappedKeyEntries;
    }

    private StsKeyEntry mapFromEntity(KeyStore.Entry keyEntry, JpaKeyEntryAttributes keyEntryAttributes) {
        return StsKeyEntry.builder()
                .alias(keyEntryAttributes.getAlias())
                .createdAt(keyEntryAttributes.getCreatedAt())
                .notBefore(keyEntryAttributes.getNotBefore())
                .notAfter(keyEntryAttributes.getNotAfter())
                .expireAt(keyEntryAttributes.getExpireAt())
                .validityInterval(keyEntryAttributes.getValidityInterval())
                .legacyInterval(keyEntryAttributes.getLegacyInterval())
                .state(keyEntryAttributes.getState())
                .keyUsage(keyEntryAttributes.getKeyUsage())
                .keyEntry(keyEntry)
                .build();
    }

    public void mapIntoEntity(StsKeyEntry stsKeyEntry, JpaKeyEntryAttributes keyEntryAttributes) {
        keyEntryAttributes.setAlias(stsKeyEntry.getAlias());
        keyEntryAttributes.setCreatedAt(stsKeyEntry.getCreatedAt());
        keyEntryAttributes.setNotBefore(stsKeyEntry.getNotBefore());
        keyEntryAttributes.setNotAfter(stsKeyEntry.getNotAfter());
        keyEntryAttributes.setExpireAt(stsKeyEntry.getExpireAt());
        keyEntryAttributes.setValidityInterval(stsKeyEntry.getValidityInterval());
        keyEntryAttributes.setLegacyInterval(stsKeyEntry.getLegacyInterval());
        keyEntryAttributes.setState(stsKeyEntry.getState());
        keyEntryAttributes.setKeyUsage(stsKeyEntry.getKeyUsage());
    }
}
