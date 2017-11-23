package de.adorsys.sts.resourceserver.processing;

import de.adorsys.sts.resourceserver.model.ResourceServerAndSecret;
import de.adorsys.sts.resourceserver.model.UserCredentials;
import de.adorsys.sts.resourceserver.service.UserDataService;
import org.adorsys.encobject.domain.KeyCredentials;
import org.adorsys.encobject.service.EncObjectService;
import org.adorsys.encobject.service.KeystoreNotFoundException;
import org.adorsys.encobject.userdata.ObjectMapperSPI;
import org.adorsys.encobject.userdata.ObjectPersistenceAdapter;
import org.adorsys.encobject.userdata.UserDataNamingPolicy;

import java.util.List;

public class ResourceServerProcessorService {

    private final ResourceServerProcessor resourceServerProcessor;

    private final UserDataNamingPolicy namingPolicy;

    private final EncObjectService encObjectService;

    private final ObjectMapperSPI objectMapper;

    public ResourceServerProcessorService(
            ResourceServerProcessor resourceServerProcessor,
            UserDataNamingPolicy namingPolicy,
            EncObjectService encObjectService,
            ObjectMapperSPI objectMapper
    ) {
        this.resourceServerProcessor = resourceServerProcessor;
        this.namingPolicy = namingPolicy;
        this.encObjectService = encObjectService;
        this.objectMapper = objectMapper;
    }

    public List<ResourceServerAndSecret> processResources(String[] audiences, String[] resources, String username, String password) {
        KeyCredentials keyCredentials = namingPolicy.newKeyCredntials(username, password);

        ObjectPersistenceAdapter persistenceAdapter = new ObjectPersistenceAdapter(encObjectService, keyCredentials, objectMapper);

        // Check if we have this user in the storage. If so user the record, if not create one.
        UserDataService userDataService = new UserDataService(namingPolicy, persistenceAdapter);
        if(!userDataService.hasAccount()){
            try {
                userDataService.addAccount();
            } catch (KeystoreNotFoundException e) {
                throw new IllegalStateException();
            }
        }

        // Check access
        UserCredentials loadUserCredentials = userDataService.loadUserCredentials();

        return resourceServerProcessor.processResources(audiences, resources, userDataService);
    }

    public void storeCredentials(String login, String password, String audience, String userEncKey) {
        KeyCredentials keyCredentials = namingPolicy.newKeyCredntials(login, password);

        ObjectPersistenceAdapter persistenceAdapter = new ObjectPersistenceAdapter(encObjectService, keyCredentials, objectMapper);

        // Check if we have this user in the storage. If so user the record, if not create one.
        UserDataService userDataService = new UserDataService(namingPolicy, persistenceAdapter);
        if(!userDataService.hasAccount()){
            try {
                userDataService.addAccount();
            } catch (KeystoreNotFoundException e) {
                throw new IllegalStateException();
            }
        }

        resourceServerProcessor.storeUserCredentials(userDataService, userEncKey, audience);
    }
}
