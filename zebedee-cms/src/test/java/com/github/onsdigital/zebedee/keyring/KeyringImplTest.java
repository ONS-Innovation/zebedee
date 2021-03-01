package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.cache.KeyringCache;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.keyring.KeyringImpl.COLLECTION_DESCRIPTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.COLLECTION_ID_NULL_OR_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.KEYRING_CACHE_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.NOT_INITALISED_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.PERMISSION_SERVICE_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.USER_KEYRING_LOCKED_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class KeyringImplTest {

    static final String TEST_COLLECTION_ID = "44";

    private Keyring keyring;

    @Mock
    private KeyringCache keyringCache;

    @Mock
    private User user;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription collDesc;

    @Mock
    private PermissionsService permissionsService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        KeyringImpl.init(keyringCache, permissionsService);
        keyring = KeyringImpl.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        resetInstanceToNull();
    }

    @Test
    public void testPopulateFromUser_userNull() throws KeyringException {
        // Given the user is null
        // when populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_userKeyringNull() throws Exception {
        // Given the user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsLocked() throws Exception {
        // Given the user keyring is locked
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsEmpty() throws Exception {
        // Given the user keyring is empty
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<>());

        // When populateFromUser is called
        keyring.populateFromUser(user);

        // Then the central keyring is not updated.
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_addThrowsException() throws Exception {
        // Given central keyring.add throws an exception
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<String>() {{
                    add(TEST_COLLECTION_ID);
                }});

        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(keyringCache)
                .add(any(), any());

        // When populateFromUser is called
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then the central keyring is not updated.
        verify(keyringCache, times(1)).add(any(), any());
    }

    @Test
    public void testPopulateFromUser_success() throws Exception {
        // Given a populated user keyring
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<String>() {{
                    add(TEST_COLLECTION_ID);
                }});

        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When populateFromUser is called
        keyring.populateFromUser(user);

        // Then the central keyring is updated with each entry in the user keyring.
        verify(keyringCache, times(1)).add(TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testGetInstance_notInitialised() throws Exception {
        // Given CollectionKeyring has not been initalised
        resetInstanceToNull();

        // When GetInstance is called
        // Then an exception is thrown
        KeyringException ex = assertThrows(KeyringException.class, () -> KeyringImpl.getInstance());
        assertThat(ex.getMessage(), equalTo(NOT_INITALISED_ERR));
    }

    @Test
    public void testGetInstance_success() throws KeyringException {
        // Given CollectionKeyring has been initalised

        // When GetInstance is called
        Keyring keyring = KeyringImpl.getInstance();

        // Then a non null instance is returned
        assertThat(keyring, is(notNullValue()));
    }

    @Test
    public void testInit_keyringCacheNull() throws Exception {
        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class, () -> KeyringImpl.init(null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(KEYRING_CACHE_NULL_ERR));
    }

    @Test
    public void testInit_permissionServiceNull() throws Exception {
        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class, () -> KeyringImpl.init(keyringCache, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(PERMISSION_SERVICE_NULL_ERR));
    }

    @Test
    public void testGet_userIsNull_shouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(null, null));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testGet_collectionIsNull_shouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionIsNull_shouldThrowException() {
        when(collection.getDescription())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_DESCRIPTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDNull_shouldThrowException() {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDEmpty_shouldThrowException() {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testGet_permissionsServiceThrowsException() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.hasAccessToCollection(user, collection))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
        verify(permissionsService, times(1)).hasAccessToCollection(user, collection);
    }

    @Test
    public void testGet_permissionsServiceReturnsFalse() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.hasAccessToCollection(user, collection))
                .thenReturn(false);

        SecretKey secretKey = keyring.get(user, collection);

        assertThat(secretKey, is(nullValue()));
        verify(permissionsService, times(1)).hasAccessToCollection(user, collection);
    }

    @Test
    public void testGet_keyringCacheThrowsException() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.hasAccessToCollection(user, collection))
                .thenReturn(true);

        when(keyringCache.get(TEST_COLLECTION_ID))
                .thenThrow(new KeyringException("Beep"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(user, collection));
        assertThat(ex.getMessage(), equalTo("Beep"));

        verify(permissionsService, times(1)).hasAccessToCollection(user, collection);
        verify(keyringCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_keyringCacheReturnsNull() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.hasAccessToCollection(user, collection))
                .thenReturn(true);

        when(keyringCache.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        SecretKey key = keyring.get(user, collection);

        assertThat(key, is(nullValue()));
        verify(permissionsService, times(1)).hasAccessToCollection(user, collection);
        verify(keyringCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_keyringCacheReturnsCollectionKey() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.hasAccessToCollection(user, collection))
                .thenReturn(true);

        when(keyringCache.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        SecretKey key = keyring.get(user, collection);

        assertThat(key, equalTo(secretKey));
        verify(permissionsService, times(1)).hasAccessToCollection(user, collection);
        verify(keyringCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_userIsNull_shouldThrowException()  {
        // Given user is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));

    }

    @Test
    public void testRemove_collectionIsNull_shouldThrowException()  {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testRemove_collectionDescriptionIsNull_shouldThrowException()  {
        // Given collection description is null
        when(collection.getDescription()).
                thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESCRIPTION_NULL_ERR));
    }

    @Test
    public void testRemove_collectionIdIsNull_shouldThrowException()  {
        // Given collection Id is null
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testRemove_collectionIdIsEmpty_shouldThrowException()  {
        // Given collection Id is null
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testRemove_permissionsServiceThrowsException() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(user, collDesc))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
    }

    @Test
    public void testRemove_permissionsServiceReturnsFalse() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(user, collDesc))
                .thenReturn(false);

        keyring.remove(user, collection);

        verify(permissionsService, times(1)).canEdit(user, collDesc);
    }

    @Test
    public void testRemove_keyringCacheRemovesCollectionKey() throws Exception {
        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(user, collDesc))
                .thenReturn(true);

        keyring.remove(user, collection);

        verify(permissionsService, times(1)).canEdit(user, collDesc);
        verify(keyringCache, times(1)).remove(TEST_COLLECTION_ID);
    }


    private void resetInstanceToNull() throws Exception {
        // Use some evil reflection magic to set the instance back to null for this test case.
        Field field = KeyringImpl.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }
}
