package de.adorsys.sts.persistence.mongo.mapper;

import com.googlecode.cqengine.query.QueryFactory;
import de.adorsys.keymanagement.api.keystore.KeyStoreView;
import de.adorsys.keymanagement.juggler.services.Juggler;
import de.adorsys.sts.keymanagement.model.KeyEntry;
import de.adorsys.sts.keymanagement.model.PasswordCallbackHandler;
import de.adorsys.sts.keymanagement.model.StsKeyEntry;
import de.adorsys.sts.keymanagement.model.StsKeyStore;
import de.adorsys.sts.keymanagement.service.KeyManagementProperties;
import de.adorsys.sts.persistence.mongo.entity.KeyEntryAttributesEntity;
import de.adorsys.sts.persistence.mongo.entity.KeyStoreEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyStore;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.adorsys.keymanagement.core.view.EntryViewImpl.A_ID;

@Component
public class KeyStoreEntityMapper {

    private static final ZonedDateTime DEFAULT_LAST_UPDATE = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
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

    public KeyStoreEntity mapToEntity(StsKeyStore keyStore) {
        KeyStoreEntity persistentKeyStore = new KeyStoreEntity();

        mapIntoEntity(keyStore, persistentKeyStore);

        return persistentKeyStore;
    }

    public void mapIntoEntity(StsKeyStore keyStore, KeyStoreEntity persistentKeyStore) {
        byte[] bytes = juggler.serializeDeserialize().serialize(keyStore.getKeyStore(), keyPassHandler::getPassword);

        persistentKeyStore.setName(keystoreName);
        persistentKeyStore.setKeystore(bytes);
        persistentKeyStore.setType(keyStore.getKeyStore().getType());
        persistentKeyStore.setLastUpdate(convert(keyStore.getLastUpdate()));

        Map<String, KeyEntryAttributesEntity> mappedEntryAttributes = mapToEntityMap(keyStore.getKeyEntries());
        persistentKeyStore.setEntries(mappedEntryAttributes);
    }

    private Map<String, KeyEntryAttributesEntity> mapToEntityMap(Map<String, StsKeyEntry> keyEntries) {
        return keyEntries.values().stream()
                .map(this::mapToEntity)
                .collect(Collectors.toMap(KeyEntryAttributesEntity::getAlias, Function.identity()));
    }

    private KeyEntryAttributesEntity mapToEntity(StsKeyEntry keyEntry) {
        KeyEntryAttributesEntity entryAttributes = new KeyEntryAttributesEntity();

        entryAttributes.setAlias(keyEntry.getAlias());
        entryAttributes.setCreatedAt(convert(keyEntry.getCreatedAt()));
        entryAttributes.setNotBefore(convert(keyEntry.getNotBefore()));
        entryAttributes.setNotAfter(convert(keyEntry.getNotAfter()));
        entryAttributes.setExpireAt(convert(keyEntry.getExpireAt()));
        entryAttributes.setValidityInterval(keyEntry.getValidityInterval());
        entryAttributes.setLegacyInterval(keyEntry.getLegacyInterval());
        entryAttributes.setState(keyEntry.getState());
        entryAttributes.setKeyUsage(keyEntry.getKeyUsage());

        return entryAttributes;
    }

    private Date convert(ZonedDateTime zonedDateTime) {
        if(zonedDateTime == null) {
            return null;
        }

        return Date.from(zonedDateTime.toInstant());
    }

    private ZonedDateTime convert(Date date) {
        if(date == null) {
            return null;
        }

        return date.toInstant().atZone(ZoneOffset.UTC);
    }

    private Map<String, StsKeyEntry> mapFromEntities(KeyStore keyStore, Map<String, KeyEntryAttributesEntity> persistentKeyEntries) {
        Map<String, StsKeyEntry> mappedKeyEntries = new HashMap<>();
        KeyStoreView view = juggler.readKeys().fromKeyStore(keyStore, id -> keyPassHandler.getPassword());

        for (Map.Entry<String, KeyEntryAttributesEntity> keyEntryAttributesMapEntry : persistentKeyEntries.entrySet()) {
            KeyStore.Entry keyEntry = view.entries()
                    .retrieve(QueryFactory.equal(A_ID, keyEntryAttributesMapEntry.getValue().getAlias()))
                    .toCollection().first()
                    .getEntry();

            StsKeyEntry mappedKeyEntry = mapFromEntity(keyEntry, keyEntryAttributesMapEntry.getValue());
            mappedKeyEntries.put(mappedKeyEntry.getAlias(), mappedKeyEntry);
        }

        return mappedKeyEntries;
    }

    private StsKeyEntry mapFromEntity(KeyStore.Entry keyEntry, KeyEntryAttributesEntity keyEntryAttributes) {
        return StsKeyEntry.builder()
                .alias(keyEntryAttributes.getAlias())
                .createdAt(convert(keyEntryAttributes.getCreatedAt()))
                .notBefore(convert(keyEntryAttributes.getNotBefore()))
                .notAfter(convert(keyEntryAttributes.getNotAfter()))
                .expireAt(convert(keyEntryAttributes.getExpireAt()))
                .validityInterval(keyEntryAttributes.getValidityInterval())
                .legacyInterval(keyEntryAttributes.getLegacyInterval())
                .state(keyEntryAttributes.getState())
                .keyUsage(keyEntryAttributes.getKeyUsage())
                .keyEntry(keyEntry)
                .build();
    }

    public StsKeyStore mapFromEntity(KeyStoreEntity persistentKeyStore) {
        KeyStore keyStore = juggler.serializeDeserialize()
                .deserialize(persistentKeyStore.getKeystore(), keyPassHandler::getPassword);

        Map<String, StsKeyEntry> mappedKeyEntries = mapFromEntities(keyStore, persistentKeyStore.getEntries());
        Date lastUpdate = persistentKeyStore.getLastUpdate();

        return StsKeyStore.builder()
                .keyStore(keyStore)
                .keyEntries(mappedKeyEntries)
                .lastUpdate(mapLastUpdate(lastUpdate))
                .build();
    }

    public ZonedDateTime mapLastUpdate(KeyStoreEntity keyStoreEntityWithLastUpdate) {
        Date lastUpdate = keyStoreEntityWithLastUpdate.getLastUpdate();
        return mapLastUpdate(lastUpdate);
    }

    private ZonedDateTime mapLastUpdate(Date lastUpdateAsDate) {
        if(lastUpdateAsDate == null) {
            return DEFAULT_LAST_UPDATE;
        }

        return convert(lastUpdateAsDate);
    }
}
