package com.github.onsdigital.zebedee.json.publishing.request;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ManifestTest {

    Zebedee zebedee;
    Collection collection;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder();
        zebedee = new Zebedee(builder.zebedee, false);
        collection = new Collection(builder.collections.get(1), zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void shouldGetManifestPathForCollection() {

        // Given a collection

        // When the getManifestPath method is called
        Path path = Manifest.getManifestPath(collection);

        // Then the expected path is returned
        assertEquals(collection.path.resolve(Manifest.filename), path);
    }

    @Test
    public void shouldSaveAndLoadManifest() throws IOException {

        // Given a new manifest
        Manifest manifest = new Manifest();
        manifest.addFileCopy("from/here", "to/there");

        // When the save method is called
        boolean saved = Manifest.save(manifest, collection);

        // Then we can load the manifest and it has the same content as the saved manifest.
        Manifest loadedManifest = Manifest.load(collection);

        assertTrue(saved);
        assertEquals(manifest.filesToCopy.size(), loadedManifest.filesToCopy.size());
        assertEquals(manifest.filesToCopy.get(0).source, loadedManifest.filesToCopy.get(0).source);
        assertEquals(manifest.filesToCopy.get(0).target, loadedManifest.filesToCopy.get(0).target);
    }
}
