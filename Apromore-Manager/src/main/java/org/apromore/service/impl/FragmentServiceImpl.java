package org.apromore.service.impl;

import org.apromore.common.Constants;
import org.apromore.dao.FragmentVersionDagRepository;
import org.apromore.dao.FragmentVersionRepository;
import org.apromore.dao.model.Content;
import org.apromore.dao.model.FragmentVersion;
import org.apromore.dao.model.FragmentVersionDag;
import org.apromore.dao.model.ProcessModelVersion;
import org.apromore.exception.ExceptionDao;
import org.apromore.exception.LockFailedException;
import org.apromore.exception.RepositoryException;
import org.apromore.graph.canonical.Canonical;
import org.apromore.service.CanonicalConverter;
import org.apromore.service.CanoniserService;
import org.apromore.service.ComposerService;
import org.apromore.service.FragmentService;
import org.apromore.service.LockService;
import org.apromore.toolbox.clustering.algorithms.dbscan.FragmentDataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;

/**
 * Implementation of the FragmentService Contract.
 *
 * @author <a href="mailto:cam.james@gmail.com">Cameron James</a>
 */
@Service
@Transactional
public class FragmentServiceImpl implements FragmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FragmentServiceImpl.class);

    private FragmentVersionRepository fvRepository;
    private FragmentVersionDagRepository fvdRepository;
    private CanonicalConverter converter;
    private CanoniserService canService;
    private ComposerService compService;
    private LockService lService;

    /**
     * Default Constructor allowing Spring to Autowire for testing and normal use.
     * @param fragmentVersionRepository Fragment Version repository.
     * @param fragmentVersionDagRepository Fragment Version Dag repository.
     * @param canonicalConverter Canonical Converter.
     * @param canoniserService Canoniser Service.
     * @param composerService The composer Service
     * @param lockService Lock Service.
     */
    @Inject
    public FragmentServiceImpl(final FragmentVersionRepository fragmentVersionRepository,
            final FragmentVersionDagRepository fragmentVersionDagRepository, final CanoniserService canoniserService,
            final LockService lockService, final ComposerService composerService, final CanonicalConverter canonicalConverter) {
        fvRepository = fragmentVersionRepository;
        fvdRepository = fragmentVersionDagRepository;
        converter = canonicalConverter;
        canService = canoniserService;
        compService = composerService;
        lService = lockService;
    }



    /**
     * @see FragmentService#getFragmentAsEPML(Integer)
     * {@inheritDoc}
     */
    @Override
    public String getFragmentAsEPML(Integer fragmentId) throws RepositoryException {
        String xml;
        try {
            Canonical g = getFragment(fragmentId, false);
            xml = canService.CPFtoString(converter.convert(g));
        } catch (LockFailedException e) {
            throw new RepositoryException(e);
        } catch (JAXBException e) {
            throw new RepositoryException(e);
        }
        return xml;
    }

    /**
     * @see FragmentService#getFragment(Integer, boolean)
     * {@inheritDoc}
     */
    @Override
    public Canonical getFragment(Integer fragmentId, boolean lock) throws LockFailedException {
        Canonical processModelGraph = null;
        try {
            if (lock) {
                LOGGER.debug("Obtaining a lock for the fragment " + fragmentId + "...");
                boolean locked = lService.lockFragment(fragmentId);
                if (!locked) {
                    throw new LockFailedException();
                }
            }

            LOGGER.debug("Composing the fragment " + fragmentId + "...");
            FragmentVersion fv = fvRepository.findOne(fragmentId);
            processModelGraph = compService.compose(fv);
            processModelGraph.setProperty(Constants.ORIGINAL_FRAGMENT_ID, fragmentId.toString());

            if (lock) {
                processModelGraph.setProperty(Constants.LOCK_STATUS, Constants.LOCKED);
            }
        } catch (ExceptionDao e) {
            String msg = "Failed to retrieve the fragment " + fragmentId;
            LOGGER.error(msg, e);
            return processModelGraph;
        }

        return processModelGraph;
    }

    /**
     * @see FragmentService#getFragmentVersion(Integer)
     * {@inheritDoc}
     */
    @Override
    public FragmentVersion getFragmentVersion(Integer fragmentVersionId) {
        return fvRepository.findOne(fragmentVersionId);
    }

    /**
     * @see FragmentService#addFragmentVersion(org.apromore.dao.model.ProcessModelVersion, org.apromore.dao.model.Content, java.util.Map, String, int, int, int, String)
     */
    @Override
    public FragmentVersion addFragmentVersion(ProcessModelVersion processModel, Content content, Map<String, String> childMappings,
            String derivedFrom, int lockStatus, int lockCount, int originalSize, String fragmentType) {
        String childMappingCode = calculateChildMappingCode(childMappings);

        FragmentVersion fragVersion = new FragmentVersion();
        fragVersion.setContent(content);
        fragVersion.setUri(UUID.randomUUID().toString());
        fragVersion.setChildMappingCode(childMappingCode);
        fragVersion.setDerivedFromFragment(derivedFrom);
        fragVersion.setLockStatus(lockStatus);
        fragVersion.setLockCount(lockCount);
        fragVersion.setFragmentType(fragmentType);
        fragVersion.setFragmentSize(originalSize);
        fragVersion.getProcessModelVersions().add(processModel) ;
        processModel.getFragmentVersions().add(fragVersion);

        addChildMappings(processModel, fragVersion, childMappings);

        return fvRepository.save(fragVersion);
    }


    /**
     * @see FragmentService#getFragment(String, boolean)
     *      {@inheritDoc}
     */
    @Override
    public Canonical getFragment(final String fragmentUri, final boolean lock) throws LockFailedException {
        if (lock) {
            boolean locked = lService.lockFragmentByUri(fragmentUri);
            if (!locked) {
                throw new LockFailedException();
            }
        }

        Canonical processModelGraph = null;
        try {

            processModelGraph = compService.compose(fvRepository.findFragmentVersionByUri(fragmentUri));
            processModelGraph.setProperty(Constants.ORIGINAL_FRAGMENT_ID, fragmentUri);
            if (lock) {
                processModelGraph.setProperty(Constants.LOCK_STATUS, Constants.LOCKED);
            }
        } catch (ExceptionDao e) {
            String msg = "Failed to retrieve the fragment " + fragmentUri;
            LOGGER.error(msg, e);
        }
        return processModelGraph;
    }

    @Override
    public FragmentVersion getMatchingFragmentVersionId(Integer contentId, Map<String, String> childMappings) {
        String childMappingCode = calculateChildMappingCode(childMappings);
        return fvRepository.getMatchingFragmentVersionId(contentId, childMappingCode);
    }

    /**
     * @see FragmentService#getUnprocessedFragments()
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<FragmentDataObject> getUnprocessedFragments() {
        List<FragmentDataObject> fragments = new ArrayList<FragmentDataObject>();
        List<FragmentVersion> fvs = fvRepository.findAll();
        for (FragmentVersion fv : fvs) {
            FragmentDataObject fragment = new FragmentDataObject();
            fragment.setFragment(fv);
            fragment.setSize(fv.getFragmentSize());
            fragments.add(fragment);
        }
        return fragments;
    }

    /**
     * @see FragmentService#getUnprocessedFragmentsOfProcesses(java.util.List)
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<FragmentDataObject> getUnprocessedFragmentsOfProcesses(final List<Integer> processIds) {
        List<FragmentDataObject> fragments = new ArrayList<FragmentDataObject>();
        List<FragmentVersion> fvs = fvRepository.getFragmentsByProcessIds(processIds);
        for (FragmentVersion fv : fvs) {
            FragmentDataObject fragment = new FragmentDataObject();
            fragment.setFragment(fv);
            fragment.setSize(fv.getFragmentSize());
            fragments.add(fragment);
        }
        return fragments;
    }


    @Override
    public void deleteChildRelationships(Integer fvid) {
        fvRepository.delete(fvid);
    }



    private String calculateChildMappingCode(Map<String, String> childMapping) {
        StringBuilder buf = new StringBuilder();
        Set<String> pids = childMapping.keySet();
        PriorityQueue<String> q = new PriorityQueue<String>(pids);
        while (!q.isEmpty()) {
            String pid = q.poll();
            String cid = childMapping.get(pid);
            buf.append(pid).append(":").append(cid).append("|");
        }
        return buf.toString();
    }

    /* Add the Fragment Version DAG */
    private void addChildMappings(ProcessModelVersion processModel, FragmentVersion fragVer, Map<String, String> childMappings) {
        Set<String> pocketIds = childMappings.keySet();
        for (String pocketId : pocketIds) {
            String childId = childMappings.get(pocketId);
            if (fragVer == null || childId == null || pocketId == null) {
                LOGGER.error("Invalid child mapping parameters. child Id: " + childId + ", Pocket Id: " + pocketId);
            }

            FragmentVersionDag fvd = new FragmentVersionDag();
            fvd.setPocketId(pocketId);
            fvd.setFragmentVersion(fragVer);
            //fvd.setChildFragmentVersion(fvRepository.findFragmentVersionByUri(childId));

            for (FragmentVersion fv : processModel.getFragmentVersions()) {
                if (fv.getUri().equals(childId)) {
                    fvd.setChildFragmentVersion(fv);
                }
            }

            fvdRepository.save(fvd);
        }
    }

}