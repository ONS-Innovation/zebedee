package com.github.onsdigital.zebedee.session.service;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.TestUtils;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.LegacySession;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.SessionsStoreImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test verify behaviour or {@link SessionsServiceImpl}.
 */
public class SessionsServiceServiceImplTest {

    private static final String EMAIL = "TEST@ons.gov.uk";
    private static final String PWD = "1 2 3 4";
    private static final String SESSION_ID = "1234567890";

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Mock
    private Supplier<String> randomIdGenerator;

    @Mock
    private SessionsStoreImpl sessionsStore;

    @Mock
    private LegacySession legacySessionMock;

    private Credentials credentials;
    private SessionsServiceImpl sessionsServiceImpl;

    @BeforeClass
    public static void setup() {
        TestUtils.initReaderConfig();
    }

    @AfterClass
    public static void tearDown() {
        TestUtils.clearReaderConfig();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        rootDir.create();

        credentials = new Credentials();
        credentials.email = EMAIL;
        credentials.password = PWD;

        sessionsServiceImpl = new SessionsServiceImpl(sessionsStore);

        when(randomIdGenerator.get())
                .thenReturn(SESSION_ID);

        ReflectionTestUtils.setField(sessionsServiceImpl, "randomIdGenerator", randomIdGenerator);
        ReflectionTestUtils.setField(sessionsServiceImpl, "sessionsStore", sessionsStore);
    }

    @Test
    public void shouldCreateSession() throws IOException, NotFoundException, BadRequestException, ClassNotFoundException {
        Session actual = sessionsServiceImpl.create(EMAIL);

        assertEquals(SESSION_ID, actual.getId());
        assertEquals(EMAIL, actual.getEmail());
        verify(sessionsStore, times(1)).write(any(LegacySession.class));
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void shouldNotCreateDuplicateSession() throws IOException, NotFoundException, BadRequestException {
        ReflectionTestUtils.setField(sessionsServiceImpl, "sessionsStore", sessionsStore);

        when(sessionsStore.read(any()))
                .thenReturn(null);
        when(legacySessionMock.getEmail())
                .thenReturn(EMAIL);
        when(legacySessionMock.getLastAccess())
                .thenReturn(new Date());

        sessionsServiceImpl.create(EMAIL);

        when(sessionsStore.find(EMAIL))
                .thenReturn(legacySessionMock);

        when(sessionsStore.read(any()))
                .thenReturn(legacySessionMock);

        sessionsServiceImpl.create(EMAIL);

        verify(sessionsStore, times(2)).find(EMAIL);
        verify(sessionsStore, times(1)).write(legacySessionMock);
    }

    @Test
    public void shouldGetSession() throws IOException, NotFoundException, BadRequestException {
        // Create a session.
        randomIdGenerator = () -> Random.id();
        ReflectionTestUtils.setField(sessionsServiceImpl, "randomIdGenerator", randomIdGenerator);


        // create a session.
        Session expected = new Session(SESSION_ID, EMAIL, new ArrayList<>());
        LegacySession storedSession = new LegacySession(SESSION_ID, EMAIL);

        when(sessionsStore.exists(SESSION_ID))
                .thenReturn(true);
        when(sessionsStore.read(SESSION_ID))
                .thenReturn(storedSession);

        assertThat(sessionsServiceImpl.get(SESSION_ID), equalTo(expected));

        verify(sessionsStore, times(1)).exists(SESSION_ID);
        verify(sessionsStore, times(1)).read(SESSION_ID);
    }

    @Test
    public void shouldNotGetNonexistentSession() throws IOException, NotFoundException, BadRequestException {
        when(sessionsStore.find(EMAIL))
                .thenReturn(null);

        assertThat(sessionsStore.find(EMAIL), equalTo(null));
        verify(sessionsStore, times(1)).find(EMAIL);
        verify(sessionsStore, never()).write(any(LegacySession.class));
    }

    @Test
    public void shouldReturnNullIfEmailIsEmptyOfNull() throws IOException, NotFoundException, BadRequestException {
        Session result = sessionsServiceImpl.create(null);

        assertNull(result);
        verify(sessionsStore, never()).write(any(LegacySession.class));
    }

    @Test
    public void shouldExpireSessions() throws IOException, InterruptedException, NotFoundException, BadRequestException {
        List<LegacySession> expired = new ArrayList<>();
        expired.add(legacySessionMock);

        when(sessionsStore.filterSessions(any(Predicate.class)))
                .thenReturn(expired);
        when(legacySessionMock.getId())
                .thenReturn(SESSION_ID);

        sessionsServiceImpl.deleteExpiredSessions();

        verify(sessionsStore, times(1)).filterSessions(any(Predicate.class));
        verify(legacySessionMock, times(1)).getId();
        verify(sessionsStore, times(1)).delete(SESSION_ID);
    }
}
