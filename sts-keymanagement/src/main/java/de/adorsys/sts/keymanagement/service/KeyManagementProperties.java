package de.adorsys.sts.keymanagement.service;

public interface KeyManagementProperties {
    KeyManagementProperties.PersistenceProperties getPersistence();
    KeyManagementProperties.KeyStoreProperties getKeystore();

    interface PersistenceProperties {
        String getContainerName();
        String getPassword();
    }

    interface KeyStoreProperties {
        String getPassword();
        String getType();
        String getName();
        String getAliasPrefix();
        KeyManagementProperties.KeyStoreProperties.KeysProperties getKeys();

        interface KeysProperties {
            KeyManagementProperties.KeyStoreProperties.KeysProperties.KeyPairProperties getEncKeyPairs();
            KeyManagementProperties.KeyStoreProperties.KeysProperties.KeyPairProperties getSignKeyPairs();
            KeyManagementProperties.KeyStoreProperties.KeysProperties.SecretKeyProperties getSecretKeys();

            interface KeyPairProperties {
                KeyRotationProperties getRotation();
                Integer getInitialCount();
                String getAlgo();
                String getSigAlgo();
                Integer getSize();
                String getName();
            }

            interface SecretKeyProperties {
                KeyRotationProperties getRotation();
                Integer getInitialCount();
                String getAlgo();
                Integer getSize();
            }

            interface KeyRotationProperties {
                Long getValidityInterval();
                Long getLegacyInterval();
                Integer getMinKeys();
                Boolean isEnabled();
            }
        }
    }
}
