package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.CREATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.DELETE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.READ;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionType.UPDATE;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CMDPermissionsServiceImplTest {

    static final String SESSION_ID = "217"; // Overlook Hotel room ...
    static final String SERVICE_TOKEN = "Union_Aerospace_Corporation"; // DOOM \m/
    static final String DATASET_ID = "Ohhh get schwifty";
    static final String COLLECTION_ID = "666";

    @Mock
    private Collections collectionsService;

    @Mock
    private SessionsService sessionsService;

    @Mock
    private ServiceStore serviceStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private Session session;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    private CMDPermissionsServiceImpl service;
    private CollectionDataset dataset;
    private HashSet datasets;
    private CRUD none;
    private CRUD fullPermissions;
    private CRUD readOnly;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);
        datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription()).thenReturn(description);

        service = new CMDPermissionsServiceImpl(sessionsService, collectionsService, serviceStore,
                permissionsService);

        none = new CRUD();
        fullPermissions = new CRUD().permit(CREATE, READ, UPDATE, DELETE);
        readOnly = new CRUD().permit(READ);
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionIDNull() throws Exception {
        try {
            service.getSessionByID(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));

            verifyZeroInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_sessionNotFound() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(null);

        try {
            service.getSessionByID(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_IOException() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenThrow(new IOException(""));

        try {
            service.getSessionByID(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verifyNoMoreInteractions(sessionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetSession_expired() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(true);

        try {
            service.getSessionByID(SESSION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));

            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            throw ex;
        }
    }

    @Test
    public void testGetSession_success() throws Exception {
        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);

        when(sessionsService.expired(session))
                .thenReturn(false);

        Session result = service.getSessionByID(SESSION_ID);
        assertThat(result, equalTo(session));

        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenNull() throws Exception {
        try {
            service.getServiceAccountByID(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_tokenEmpty() throws Exception {
        try {
            service.getServiceAccountByID("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_IOException() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException(""));

        try {
            service.getServiceAccountByID(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceAccount_notFound() throws Exception {
        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(null);

        try {
            service.getServiceAccountByID(SERVICE_TOKEN);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test
    public void testGetServiceAccount_success() throws Exception {
        ServiceAccount expected = new ServiceAccount(SERVICE_TOKEN);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(expected);

        ServiceAccount actual = service.getServiceAccountByID(SERVICE_TOKEN);
        assertThat(actual, equalTo(expected));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testCollectionContainsDataset_false() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(new HashSet<>());

        assertFalse(service.collectionContainsDataset(collection, DATASET_ID));
    }

    @Test
    public void testCollectionContainsDataset_true() throws Exception {
        CollectionDataset dataset = new CollectionDataset();
        dataset.setId(DATASET_ID);

        HashSet datasets = new HashSet<CollectionDataset>() {{
            add(dataset);
        }};

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getDatasets())
                .thenReturn(datasets);

        assertTrue(service.collectionContainsDataset(collection, DATASET_ID));
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idNull() throws Exception {
        try {
            service.getCollectionByID(null);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_idEmpty() throws Exception {
        try {
            service.getCollectionByID("");
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(collectionsService);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_IOException() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenThrow(new IOException());

        try {
            service.getCollectionByID(COLLECTION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetCollection_notFOund() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(null);

        try {
            service.getCollectionByID(COLLECTION_ID);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_NOT_FOUND));
            verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
            throw ex;
        }
    }

    @Test
    public void testGetCollection_success() throws Exception {
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        Collection actual = service.getCollectionByID(COLLECTION_ID);
        assertThat(actual, equalTo(collection));
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenNull() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, null, DATASET_ID, null);
        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_tokenEmpty() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, "", DATASET_ID, null);
        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verifyZeroInteractions(serviceStore);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_notFound() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(null);

        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_UNAUTHORIZED));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test(expected = PermissionsException.class)
    public void testGetServiceDatasetPermissions_IOException() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenThrow(new IOException());

        try {
            service.getServiceDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            verify(serviceStore, times(1)).get(SERVICE_TOKEN);
            throw ex;
        }
    }

    @Test
    public void testGetServiceDatasetPermissions_success() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(null, SERVICE_TOKEN, DATASET_ID, null);

        ServiceAccount expected = new ServiceAccount(SERVICE_TOKEN);

        when(serviceStore.get(SERVICE_TOKEN))
                .thenReturn(expected);

        CRUD result = service.getServiceDatasetPermissions(request);

        assertThat(result, equalTo(fullPermissions));
        verify(serviceStore, times(1)).get(SERVICE_TOKEN);
    }

    @Test
    public void testGetUserDatasetPermissions_editorUser() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(SESSION_ID, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(permissionsService.canEdit(session))
                .thenReturn(true);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(fullPermissions));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerUserAssignedToCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(SESSION_ID, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);
        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, description))
                .thenReturn(true);
        when(description.getDatasets())
                .thenReturn(datasets);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(readOnly));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, description);
    }

    @Test(expected = PermissionsException.class)
    public void testGetUserDatasetPermissions_viewerCollectionIDNull() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(SESSION_ID, SERVICE_TOKEN, null, null);

        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(permissionsService.canEdit(session))
                .thenReturn(false);

        try {
            service.getUserDatasetPermissions(request);
        } catch (PermissionsException ex) {
            assertThat(ex.statusCode, equalTo(HttpStatus.SC_BAD_REQUEST));
            verify(sessionsService, times(1)).get(SESSION_ID);
            verify(sessionsService, times(1)).expired(session);
            verify(collectionsService, never()).getCollection(anyString());
            throw ex;
        }
    }

    @Test
    public void testGetUserDatasetPermissions_viewerCannotViewCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(SESSION_ID, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, description))
                .thenReturn(false);
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(none));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, description);
    }

    @Test
    public void testGetUserDatasetPermissions_viewerDatasetNotInCollection() throws Exception {
        GetPermissionsRequest request = new GetPermissionsRequest(SESSION_ID, SERVICE_TOKEN, DATASET_ID, COLLECTION_ID);

        when(sessionsService.get(SESSION_ID))
                .thenReturn(session);
        when(permissionsService.canEdit(session))
                .thenReturn(false);
        when(permissionsService.canView(session, description))
                .thenReturn(true);
        when(collectionsService.getCollection(COLLECTION_ID))
                .thenReturn(collection);
        when(description.getDatasets())
                .thenReturn(new HashSet<>());

        CRUD actual = service.getUserDatasetPermissions(request);

        assertThat(actual, equalTo(none));
        verify(sessionsService, times(1)).get(SESSION_ID);
        verify(sessionsService, times(1)).expired(session);
        verify(collectionsService, times(1)).getCollection(COLLECTION_ID);
        verify(permissionsService, times(1)).canEdit(session);
        verify(permissionsService, times(1)).canView(session, description);
    }

    @Test
    public void testUserHasEditCollectionPermission_true() throws Exception {
        when(permissionsService.canEdit(session))
                .thenReturn(true);

        assertTrue(service.userHasEditCollectionPermission(session));

        verify(permissionsService, times(1))
                .canEdit(session);
    }

    @Test
    public void testUserHasEditCollectionPermission_false() throws Exception {
        when(permissionsService.canEdit(session))
                .thenReturn(false);

        assertFalse(service.userHasEditCollectionPermission(session));

        verify(permissionsService, times(1))
                .canEdit(session);
    }

    @Test(expected = PermissionsException.class)
    public void testUserHasEditCollectionPermission_IOException() throws Exception {
        when(permissionsService.canEdit(session))
                .thenThrow(new IOException());

        try {
            service.userHasEditCollectionPermission(session);
        } catch (PermissionsException ex) {
            verify(permissionsService, times(1))
                    .canEdit(session);
            PermissionsException expected = internalServerErrorException();
            assertThat(expected.statusCode, equalTo(ex.statusCode));
            assertThat(expected.getMessage(), equalTo(ex.getMessage()));
            throw ex;
        }
    }

    @Test
    public void testUserHasViewCollectionPermission_true() throws Exception {
        when(permissionsService.canView(session, description))
                .thenReturn(true);

        assertTrue(service.userHasViewCollectionPermission(session, description));

        verify(permissionsService, times(1))
                .canView(session, description);
    }

    @Test
    public void testUserHasViewCollectionPermission_false() throws Exception {
        when(permissionsService.canView(session, description))
                .thenReturn(false);

        assertFalse(service.userHasViewCollectionPermission(session, description));

        verify(permissionsService, times(1))
                .canView(session, description);
    }

    @Test(expected = PermissionsException.class)
    public void testUserHasViewCollectionPermission_IOException() throws Exception {
        when(permissionsService.canView(session, description))
                .thenThrow(new IOException());

        try {
            service.userHasViewCollectionPermission(session, description);
        } catch (PermissionsException ex) {
            verify(permissionsService, times(1))
                    .canView(session, description);
            PermissionsException expected = internalServerErrorException();
            assertThat(expected.statusCode, equalTo(ex.statusCode));
            assertThat(expected.getMessage(), equalTo(ex.getMessage()));
            throw ex;
        }
    }
}
