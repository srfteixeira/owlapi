package org.semanticweb.owlapi.impltest.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.owlapi.apitest.baseclasses.TestBase;
import org.semanticweb.owlapi.impl.OWLImportsDeclarationImpl;
import org.semanticweb.owlapi.impl.OWLOntologyImpl;
import org.semanticweb.owlapi.impl.OWLOntologyManagerImpl;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.OWLStorerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.ImpendingOWLOntologyChangeListener;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeBroadcastStrategy;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyChangeProgressListener;
import org.semanticweb.owlapi.model.OWLOntologyChangesVetoedListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OntologyConfigurator;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 13/04/15
 */
@RunWith(MockitoJUnitRunner.class)
public class OWLOntologyManager_Concurrent_TestCase extends TestBase {

    private static final String HTTP_OWLAPI = "http://owlapi/";
    private OWLOntologyManager manager;
    @Mock
    private Lock readLock, writeLock;
    @Mock
    private ReadWriteLock readWriteLock;
    private OWLOntology ontology;

    @Before
    public void setUp() throws Exception {
        when(readWriteLock.readLock()).thenReturn(readLock);
        when(readWriteLock.writeLock()).thenReturn(writeLock);
        manager = new OWLOntologyManagerImpl(df, readWriteLock);
        mockAndAddOntologyFactory();
        IRI iri = df.getIRI(HTTP_OWLAPI, "ont");
        ontology = manager.createOntology(iri);
        manager.setOntologyDocumentIRI(ontology, iri);
        reset(readLock, writeLock, readWriteLock);
    }

    @SuppressWarnings("boxing")
    private void mockAndAddOntologyFactory() throws OWLOntologyCreationException {
        OWLOntologyFactory ontologyFactory = mock(OWLOntologyFactory.class);
        when(ontologyFactory.canCreateFromDocumentIRI(any(IRI.class))).thenReturn(Boolean.TRUE);
        when(ontologyFactory.canAttemptLoading(any(OWLOntologyDocumentSource.class)))
            .thenReturn(Boolean.TRUE);
        final OWLOntology owlOntology =
            new OWLOntologyImpl(manager, df.getOWLOntologyID(), manager.getOntologyConfigurator());
        when(ontologyFactory.createOWLOntology(any(OWLOntologyManager.class),
            any(OWLOntologyID.class), any(IRI.class),
            any(OWLOntologyFactory.OWLOntologyCreationHandler.class),
            any(OntologyConfigurator.class))).thenAnswer(i -> notify(3, i, owlOntology));
        when(ontologyFactory.loadOWLOntology(any(OWLOntologyManager.class),
            any(OWLOntologyDocumentSource.class),
            any(OWLOntologyFactory.OWLOntologyCreationHandler.class),
            any(OntologyConfigurator.class))).thenAnswer(i -> notify(2, i, owlOntology));
        manager.setOntologyFactories(Collections.singleton(ontologyFactory));
    }

    private static OWLOntology notify(int i, InvocationOnMock o, OWLOntology ont) {
        ((OWLOntologyFactory.OWLOntologyCreationHandler) o.getArguments()[i]).ontologyCreated(ont);
        return ont;
    }

    @Test
    public void shouldCall_contains_with_readLock() {
        IRI arg0 = mockIRI();
        manager.contains(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_contains_with_readLock_2() {
        OWLOntologyID arg0 = df.getOWLOntologyID(df.getIRI("urn:test:", "ontology"));
        manager.contains(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_contains_with_no_readLock_onAnonymous() {
        // anonymous ontology ids are never contained, no need to engage locks
        OWLOntologyID arg0 = df.getOWLOntologyID();
        manager.contains(arg0);
        verify(readLock, never()).lock();
        verify(readLock, never()).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }

    @Test
    public void shouldCall_contains_with_readLock_3() {
        manager.contains(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_containsVersion_with_readLock() {
        IRI arg0 = mockIRI();
        manager.containsVersion(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntology_with_readLock() {
        OWLOntologyID arg0 = df.getOWLOntologyID();
        manager.getOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntology_with_readLock_2() {
        IRI arg0 = mockIRI();
        manager.getOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    private static IRI mockIRI() {
        return df.getIRI("http://owlapi.sourceforge.net/", "stuff");
    }

    @Test
    public void shouldCall_getImportedOntology_with_readLock() {
        OWLImportsDeclaration arg0 = new OWLImportsDeclarationImpl(df.getIRI(HTTP_OWLAPI, "ont"));
        manager.getImportedOntology(arg0);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_getSortedImportsClosure_with_readLock() {
        manager.getSortedImportsClosure(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_2() throws OWLOntologyCreationException {
        OWLOntologyID arg0 = df.getOWLOntologyID();
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_3() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        Set<OWLOntology> arg1 = Collections.emptySet();
        boolean arg2 = true;
        manager.createOntology(arg0, arg1, arg2);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_4() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        Set<OWLOntology> arg1 = new HashSet<>();
        manager.createOntology(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_5() throws OWLOntologyCreationException {
        manager.createOntology();
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_6() throws OWLOntologyCreationException {
        Set<OWLAxiom> arg0 = Collections.singleton(mock(OWLAxiom.class));
        manager.createOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_createOntology_with_writeLock_7() throws OWLOntologyCreationException {
        Set<OWLAxiom> arg0 = Collections.emptySet();
        IRI arg1 = mockIRI();
        manager.createOntology(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_loadOntology_with_writeLock() throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.loadOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_loadOntologyFromOntologyDocument_with_writeLock()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource arg0 = mock(OWLOntologyDocumentSource.class);
        when(arg0.getDocumentIRI()).thenReturn("http://owlapi/ontdoc");
        OntologyConfigurator arg1 = mock(OntologyConfigurator.class);
        manager.loadOntologyFromOntologyDocument(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_2()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource arg0 = mock(OWLOntologyDocumentSource.class);
        when(arg0.getDocumentIRI()).thenReturn("http://owlapi/ontdoc");
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_3()
        throws OWLOntologyCreationException {
        InputStream arg0 = new ByteArrayInputStream("test".getBytes());
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_4()
        throws OWLOntologyCreationException {
        OWLOntologyDocumentSource source = mock(OWLOntologyDocumentSource.class);
        when(source.getDocumentIRI()).thenReturn("http://owlapi/ontdoc");
        manager.loadOntologyFromOntologyDocument(source);
        verifyWriteLock_LockUnlock();
    }

    private void verifyWriteLock_LockUnlock() {
        InOrder inOrder = Mockito.inOrder(writeLock, writeLock);
        inOrder.verify(writeLock, atLeastOnce()).lock();
        inOrder.verify(writeLock, atLeastOnce()).unlock();
    }

    @Test
    public void shouldCall_loadOntologyFromOntologyDocument_with_writeLock_5()
        throws OWLOntologyCreationException {
        IRI arg0 = mockIRI();
        manager.loadOntologyFromOntologyDocument(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntology_with_writeLock() {
        OWLOntologyID arg0 = mock(OWLOntologyID.class);
        manager.removeOntology(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntology_with_writeLock_2() {
        manager.removeOntology(ontology);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyDocumentIRI_with_readLock() {
        manager.getOntologyDocumentIRI(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyDocumentIRI_with_writeLock() {
        IRI arg1 = mockIRI();
        manager.setOntologyDocumentIRI(ontology, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyFormat_with_readLock() {
        manager.getOntologyFormat(ontology);
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyFormat_with_writeLock() {
        OWLDocumentFormat arg1 = mock(OWLDocumentFormat.class);
        manager.setOntologyFormat(ontology, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setIRIMappers_with_writeLock() {
        Set<OWLOntologyIRIMapper> arg0 = new HashSet<>();
        manager.setIRIMappers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getIRIMappers_with_readLock() {
        manager.getIRIMappers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldAddIRIMapper_with_writeLock() {
        manager.getIRIMappers().add(mock(OWLOntologyIRIMapper.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldRemoveIRIMapper_with_writeLock() {
        manager.getIRIMappers().remove(mock(OWLOntologyIRIMapper.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyParsers_with_writeLock() {
        Set<OWLParserFactory> arg0 = new HashSet<>();
        manager.setOntologyParsers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyParsers_with_readLock() {
        manager.getOntologyParsers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldAddOntologyParser_with_writeLock() {
        manager.getOntologyParsers().add(mock(OWLParserFactory.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldRemoveOntologyParser_with_writeLock() {
        manager.getOntologyParsers().remove(mock(OWLParserFactory.class));
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyFactories_with_writeLock() {
        Set<OWLOntologyFactory> arg0 = new HashSet<>();
        manager.setOntologyFactories(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyFactories_with_readLock() {
        manager.getOntologyFactories().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyStorers_with_writeLock() {
        Set<OWLStorerFactory> arg0 = new HashSet<>();
        manager.setOntologyStorers(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyStorers_with_readLock() {
        manager.getOntologyStorers().iterator();
        verifyReadLock_LockUnlock();
    }

    @Test
    public void shouldCall_addOntologyChangeListener_with_writeLock() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        OWLOntologyChangeBroadcastStrategy arg1 = mock(OWLOntologyChangeBroadcastStrategy.class);
        manager.addOntologyChangeListener(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_addImpendingOntologyChangeListener_with_writeLock() {
        ImpendingOWLOntologyChangeListener arg0 = mock(ImpendingOWLOntologyChangeListener.class);
        manager.addImpendingOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeImpendingOntologyChangeListener_with_writeLock() {
        ImpendingOWLOntologyChangeListener arg0 = mock(ImpendingOWLOntologyChangeListener.class);
        manager.removeImpendingOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_addOntologyChangesVetoedListener_with_writeLock() {
        OWLOntologyChangesVetoedListener arg0 = mock(OWLOntologyChangesVetoedListener.class);
        manager.addOntologyChangesVetoedListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntologyChangesVetoedListener_with_writeLock() {
        OWLOntologyChangesVetoedListener arg0 = mock(OWLOntologyChangesVetoedListener.class);
        manager.removeOntologyChangesVetoedListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setDefaultChangeBroadcastStrategy_with_writeLock() {
        OWLOntologyChangeBroadcastStrategy arg0 = mock(OWLOntologyChangeBroadcastStrategy.class);
        manager.setDefaultChangeBroadcastStrategy(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_makeLoadImportRequest_with_writeLock() {
        OWLImportsDeclaration arg0 = mock(OWLImportsDeclaration.class);
        when(arg0.getIRI()).thenReturn(df.getIRI(HTTP_OWLAPI, "other"));
        OntologyConfigurator arg1 = mock(OntologyConfigurator.class);
        manager.makeLoadImportRequest(arg0, arg1);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_makeLoadImportRequest_with_writeLock_2() {
        OWLImportsDeclaration arg0 =
            new OWLImportsDeclarationImpl(df.getIRI(HTTP_OWLAPI, "otheront"));
        manager.makeLoadImportRequest(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_addMissingImportListener_with_writeLock() {
        MissingImportListener arg0 = mock(MissingImportListener.class);
        manager.addMissingImportListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeMissingImportListener_with_writeLock() {
        MissingImportListener arg0 = mock(MissingImportListener.class);
        manager.removeMissingImportListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_addOntologyLoaderListener_with_writeLock() {
        OWLOntologyLoaderListener arg0 = mock(OWLOntologyLoaderListener.class);
        manager.addOntologyLoaderListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntologyLoaderListener_with_writeLock() {
        OWLOntologyLoaderListener arg0 = mock(OWLOntologyLoaderListener.class);
        manager.removeOntologyLoaderListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_addOntologyChangeProgessListener_with_writeLock() {
        OWLOntologyChangeProgressListener arg0 = mock(OWLOntologyChangeProgressListener.class);
        manager.addOntologyChangeProgessListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntologyChangeProgessListener_with_writeLock() {
        OWLOntologyChangeProgressListener arg0 = mock(OWLOntologyChangeProgressListener.class);
        manager.removeOntologyChangeProgessListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    // XXX use reflection instead of enumerating all methods
    protected OWLMutableOntology mockOntology() {
        OWLMutableOntology mock = mock(OWLMutableOntology.class);
        when(mock.getOntologyID()).thenReturn(df.getOWLOntologyID(
            Optional.ofNullable(df.getIRI("urn:mock:", "ontology")), Optional.empty()));
        return mock;
    }

    @Test
    public void shouldCall_addOntologyChangeListener_with_writeLock_2() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        manager.addOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_removeOntologyChangeListener_with_writeLock() {
        OWLOntologyChangeListener arg0 = mock(OWLOntologyChangeListener.class);
        manager.removeOntologyChangeListener(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyWriterConfiguration_with_writeLock() {
        OntologyConfigurator arg0 = mock(OntologyConfigurator.class);
        manager.setOntologyConfigurator(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_setOntologyLoaderConfiguration_with_writeLock() {
        OntologyConfigurator arg0 = mock(OntologyConfigurator.class);
        manager.setOntologyConfigurator(arg0);
        verifyWriteLock_LockUnlock();
    }

    @Test
    public void shouldCall_getOntologyLoaderConfiguration_with_readLock() {
        manager.getOntologyConfigurator();
        verifyReadLock_LockUnlock();
    }

    private void verifyReadLock_LockUnlock() {
        InOrder inOrder = Mockito.inOrder(readLock, readLock);
        inOrder.verify(readLock, atLeastOnce()).lock();
        inOrder.verify(readLock, atLeastOnce()).unlock();
        verify(writeLock, never()).lock();
        verify(writeLock, never()).unlock();
    }
}
