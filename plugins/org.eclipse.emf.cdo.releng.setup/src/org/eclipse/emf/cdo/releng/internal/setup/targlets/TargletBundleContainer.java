/*
 * Copyright (c) 2004-2014 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.releng.internal.setup.targlets;

import org.eclipse.emf.cdo.releng.internal.setup.Activator;
import org.eclipse.emf.cdo.releng.setup.AutomaticSourceLocator;
import org.eclipse.emf.cdo.releng.setup.InstallableUnit;
import org.eclipse.emf.cdo.releng.setup.P2Repository;
import org.eclipse.emf.cdo.releng.setup.RepositoryList;
import org.eclipse.emf.cdo.releng.setup.SetupFactory;
import org.eclipse.emf.cdo.releng.setup.Targlet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
@SuppressWarnings("restriction")
public class TargletBundleContainer extends AbstractBundleContainer
{
  public static final String TYPE = "Targlet";

  private static final String PROP_TARGLET_PROFILE = "targlet.profile";

  private static final String FOLLOW_ARTIFACT_REPOSITORY_REFERENCES = "org.eclipse.equinox.p2.director.followArtifactRepositoryReferences";

  private static final String TRUE = Boolean.TRUE.toString();

  private static final String CURRENT_AGENT_FILTER = '(' + IProvisioningAgent.SERVICE_CURRENT + '=' + TRUE + ')';

  private static final String FALSE = Boolean.FALSE.toString();

  private String profileID;

  private List<Targlet> targlets = new ArrayList<Targlet>();

  private transient IProvisioningAgent currentAgent;

  private transient String bundlePool;

  private transient IInstallableUnit[] units;

  public TargletBundleContainer(String profileID)
  {
    this.profileID = profileID;
  }

  @Override
  public String getType()
  {
    return TYPE;
  }

  public String getProfileID()
  {
    return profileID;
  }

  public List<Targlet> getTarglets()
  {
    return Collections.unmodifiableList(targlets);
  }

  public void addTarglet(Targlet targlet)
  {
    targlets.add(targlet);
  }

  @Override
  public String serialize()
  {
    return Factory.serialize(profileID, targlets);
  }

  @Override
  public String toString()
  {
    return profileID;
  }

  /*
   * Is only called by content provider and only if this targlet is resolved.
   */
  public IInstallableUnit[] getUnits() throws CoreException
  {
    return units;
  }

  private void init() throws ProvisionException
  {
    try
    {
      if (currentAgent == null)
      {
        currentAgent = getCurrentAgent();
        IProfileRegistry profileRegistry = getProfileRegistry(currentAgent);

        IProfile currentProfile = profileRegistry.getProfile(IProfileRegistry.SELF);
        if (currentProfile == null)
        {
          throw new ProvisionException("Current profile could not be loaded");
        }

        if ("SelfHostingProfile".equals(currentProfile.getProfileId()))
        {
          // cacheDir = P2TargetUtils.BUNDLE_POOL.toOSString();
          bundlePool = System.getProperty("targlet.bundle.pool");
        }
        else
        {
          bundlePool = currentProfile.getProperty(IProfile.PROP_CACHE);
        }

        if (bundlePool == null)
        {
          throw new ProvisionException("Current profile has no bundle pool");
        }
      }
    }
    catch (Throwable t)
    {
      currentAgent = null;
      bundlePool = null;
      throwProvisionException(t);
    }
  }

  @Override
  public String getLocation(boolean resolve) throws CoreException
  {
    init();
    return bundlePool;
  }

  public void updateProfile(IProgressMonitor monitor) throws ProvisionException
  {
    init();

    IProfileRegistry profileRegistry = getProfileRegistry(currentAgent);
    profileRegistry.removeProfile(profileID);

    IProvisioningAgent specialAgent = null; // ;-)

    try
    {
      specialAgent = createSpecialAgent(currentAgent);

      IProfile profile = createProfile(specialAgent, profileID);

      MetadataRepositoryManager metadataRepositoryManager = new MetadataRepositoryManager(specialAgent);
      specialAgent.registerService(IMetadataRepositoryManager.SERVICE_NAME, metadataRepositoryManager);
      for (URI uri : metadataRepositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL))
      {
        metadataRepositoryManager.removeRepository(uri);
      }

      ArtifactRepositoryManager artifactRepositoryManager = new ArtifactRepositoryManager(specialAgent);
      specialAgent.registerService(IArtifactRepositoryManager.SERVICE_NAME, artifactRepositoryManager);
      for (URI uri : artifactRepositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL))
      {
        artifactRepositoryManager.removeRepository(uri);
      }

      List<URI> uris = new ArrayList<URI>();
      for (Targlet targlet : targlets)
      {
        for (P2Repository p2Repository : targlet.getActiveP2Repositories())
        {
          try
          {
            URI uri = new URI(p2Repository.getURL());
            metadataRepositoryManager.addRepository(uri);
            artifactRepositoryManager.addRepository(uri);
          }
          catch (URISyntaxException ex)
          {
            throw new ProvisionException(ex.getMessage(), ex);
          }
        }
      }

      ProvisioningContext provisioningContext = new ProvisioningContext(specialAgent)
      {
        @Override
        public IQueryable<IInstallableUnit> getMetadata(IProgressMonitor monitor)
        {
          return super.getMetadata(monitor); // XXX
        }
      };

      URI[] uriArray = uris.toArray(new URI[uris.size()]);
      provisioningContext.setMetadataRepositories(uriArray);
      provisioningContext.setArtifactRepositories(uriArray);
      provisioningContext.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, FALSE);
      provisioningContext.setProperty(FOLLOW_ARTIFACT_REPOSITORY_REFERENCES, FALSE);

      ProfileChangeRequest request = new ProfileChangeRequest(profile);

      for (Targlet targlet : targlets)
      {
        for (InstallableUnit root : targlet.getRoots())
        {
          IQuery<IInstallableUnit> iuQuery = QueryUtil.createIUQuery(root.getID(), root.getVersionRange());
          IQuery<IInstallableUnit> latestQuery = QueryUtil.createLatestQuery(iuQuery);

          for (IInstallableUnit installableUnit : metadataRepositoryManager.query(latestQuery, monitor))
          {
            request.setInstallableUnitProfileProperty(installableUnit, IProfile.PROP_PROFILE_ROOT_IU, TRUE);
            request.add(installableUnit);
          }
        }
      }

      IPlanner planner = getPlanner(specialAgent);
      IProvisioningPlan result = planner.getProvisioningPlan(request, provisioningContext, new NullProgressMonitor());
      if (!result.getStatus().isOK())
      {
        throw new ProvisionException(result.getStatus());
      }

      IEngine engine = getEngine(specialAgent);
      IStatus status = PlanExecutionHelper.executePlan(result, engine, provisioningContext, new NullProgressMonitor());
      if (!status.isOK())
      {
        throw new ProvisionException(status);
      }

      // return new Status(IStatus.OK, PDECore.PLUGIN_ID, ITargetLocationUpdater.STATUS_CODE_NO_CHANGE,
      // "Targlet container update completed successfully", null);
    }
    finally
    {
      if (specialAgent != null)
      {
        specialAgent.stop();
      }
    }
  }

  @Override
  protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException
  {
    resolveUnits(monitor);
    return fBundles;
  }

  @Override
  protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
      throws CoreException
      {
    return fFeatures;
      }

  private void resolveUnits(IProgressMonitor monitor) throws ProvisionException
  {
    try
    {
      init();

      IProfileRegistry profileRegistry = getProfileRegistry(currentAgent);
      IProfile profile = profileRegistry.getProfile(profileID);
      if (!isValidProfile(profile))
      {
        updateProfile(monitor);
        profile = profileRegistry.getProfile(profileID);
      }

      List<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
      List<TargetBundle> bundles = new ArrayList<TargetBundle>();
      List<TargetFeature> features = new ArrayList<TargetFeature>();

      IFileArtifactRepository cache = getBundlePoolRepository();

      IQueryResult<IInstallableUnit> result = profile.query(QueryUtil.createIUAnyQuery(), null);
      for (Iterator<IInstallableUnit> i = result.iterator(); i.hasNext();)
      {
        IInstallableUnit unit = i.next();
        units.add(unit);

        if (isOSGiBundle(unit))
        {
          generateBundle(unit, cache, bundles);

          // if (getIncludeSource())
          // {
          // // bit of a hack using the bundle naming convention for finding source bundles
          // // but this matches what we do when adding source to the profile so...
          // IQuery<IInstallableUnit> sourceQuery = QueryUtil.createIUQuery(unit.getId() + ".source",
          // unit.getVersion());
          // IQueryResult<IInstallableUnit> result = metadata.query(sourceQuery, null);
          // if (!result.isEmpty())
          // {
          // generateBundle(result.iterator().next(), artifacts, bundles);
          // }
          // }
        }
        else if (isFeatureJar(unit))
        {
          generateFeature(unit, cache, features);
        }
      }

      this.units = units.toArray(new IInstallableUnit[units.size()]);
      fBundles = bundles.toArray(new TargetBundle[bundles.size()]);
      fFeatures = features.toArray(new TargetFeature[features.size()]);
    }
    catch (Throwable t)
    {
      Activator.log(t);
      throwProvisionException(t);
    }
  }

  private boolean isValidProfile(IProfile profile)
  {
    if (profile == null)
    {
      return false;
    }

    String propTarglet = profile.getProperty(PROP_TARGLET_PROFILE);
    if (!TRUE.equals(propTarglet))
    {
      return false;
    }

    String propCache = profile.getProperty(IProfile.PROP_CACHE);
    if (!bundlePool.equals(propCache))
    {
      return false;
    }

    return true;
  }

  private void generateBundle(IInstallableUnit unit, IFileArtifactRepository repo, List<TargetBundle> bundles)
      throws CoreException
      {
    Collection<IArtifactKey> artifacts = unit.getArtifacts();
    for (Iterator<IArtifactKey> iterator2 = artifacts.iterator(); iterator2.hasNext();)
    {
      File file = repo.getArtifactFile(iterator2.next());
      if (file != null)
      {
        TargetBundle bundle = new TargetBundle(file);
        bundles.add(bundle);
      }
    }
      }

  private void generateFeature(IInstallableUnit unit, IFileArtifactRepository repo, List<TargetFeature> features)
      throws CoreException
      {
    Collection<IArtifactKey> artifacts = unit.getArtifacts();
    for (Iterator<IArtifactKey> iterator2 = artifacts.iterator(); iterator2.hasNext();)
    {
      File file = repo.getArtifactFile(iterator2.next());
      if (file != null)
      {
        TargetFeature feature = new TargetFeature(file);
        features.add(feature);
      }
    }
      }

  private boolean isOSGiBundle(IInstallableUnit unit)
  {
    return providesNamespace(unit, "osgi.bundle");
  }

  private boolean isFeatureJar(IInstallableUnit unit)
  {
    return providesNamespace(unit, "org.eclipse.update.feature");
  }

  private boolean providesNamespace(IInstallableUnit unit, String namespace)
  {
    for (IProvidedCapability providedCapability : unit.getProvidedCapabilities())
    {
      if (namespace.equals(providedCapability.getNamespace()))
      {
        return true;
      }
    }

    return false;
  }

  private IProfile createProfile(IProvisioningAgent agent, String profileID) throws ProvisionException
  {
    IProfileRegistry profileRegistry = getProfileRegistry(agent);
    profileRegistry.removeProfile(profileID);

    Map<String, String> props = new HashMap<String, String>();
    // props.put(IProfile.PROP_INSTALL_FOLDER, destination); // XXX Doesn't seem to be used
    props.put(IProfile.PROP_CACHE, bundlePool);
    props.put(PROP_TARGLET_PROFILE, TRUE);
    props.put("org.eclipse.update.install.features", TRUE);

    return profileRegistry.addProfile(profileID, props);
  }

  private IFileArtifactRepository getBundlePoolRepository() throws ProvisionException
  {
    IArtifactRepositoryManager manager = (IArtifactRepositoryManager)currentAgent
        .getService(IArtifactRepositoryManager.SERVICE_NAME);
    if (manager == null)
    {
      throw new ProvisionException("Artifact respository manager could not be loaded");
    }

    URI uri = new File(bundlePool).toURI();

    try
    {
      if (manager.contains(uri))
      {
        return (IFileArtifactRepository)manager.loadRepository(uri, null);
      }
    }
    catch (ProvisionException ex)
    {
      // Could not load or there wasn't one, fall through to create
    }

    IArtifactRepository result = manager.createRepository(uri, "Shared Bundle Pool",
        IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
    return (IFileArtifactRepository)result;
  }

  private static IProvisioningAgent getCurrentAgent() throws ProvisionException
  {
    Collection<ServiceReference<IProvisioningAgent>> ref = null;
    BundleContext context = Activator.getBundleContext();

    try
    {
      ref = context.getServiceReferences(IProvisioningAgent.class, CURRENT_AGENT_FILTER);
    }
    catch (InvalidSyntaxException e)
    {
      // Can't happen because we write the filter ourselves
    }

    if (ref == null || ref.size() == 0)
    {
      throw new ProvisionException("Current provisioning agent could not be loaded");
    }

    IProvisioningAgent agent = context.getService(ref.iterator().next());
    context.ungetService(ref.iterator().next());
    return agent;
  }

  private static IAgentLocation getAgentLocation(IProvisioningAgent agent) throws ProvisionException
  {
    IAgentLocation agentLocation = (IAgentLocation)agent.getService(IAgentLocation.SERVICE_NAME);
    if (agentLocation == null)
    {
      throw new ProvisionException("Provisioning agent has no location");
    }

    return agentLocation;
  }

  private static IProfileRegistry getProfileRegistry(IProvisioningAgent agent) throws ProvisionException
  {
    IProfileRegistry profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
    if (profileRegistry == null)
    {
      throw new ProvisionException("Profile registry could not be loaded");
    }

    return profileRegistry;
  }

  private static IPlanner getPlanner(IProvisioningAgent agent) throws ProvisionException
  {
    IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);
    if (planner == null)
    {
      throw new ProvisionException("Planner could not be loaded");
    }

    return planner;
  }

  private static IEngine getEngine(IProvisioningAgent agent) throws ProvisionException
  {
    IEngine engine = (IEngine)agent.getService(IEngine.SERVICE_NAME);
    if (engine == null)
    {
      throw new ProvisionException("Engine could not be loaded");
    }

    return engine;
  }

  private static IProvisioningAgent createSpecialAgent(IProvisioningAgent currentAgent) throws ProvisionException
  {
    IAgentLocation currentLocation = getAgentLocation(currentAgent);

    BundleContext bundleContext = Activator.getBundleContext();
    ServiceReference<IProvisioningAgentProvider> providerRef = bundleContext
        .getServiceReference(IProvisioningAgentProvider.class);

    try
    {
      IProvisioningAgentProvider provider = bundleContext.getService(providerRef);
      return provider.createAgent(currentLocation.getRootLocation());
    }
    finally
    {
      bundleContext.ungetService(providerRef);
    }
  }

  private static void throwProvisionException(Throwable t) throws ProvisionException
  {
    if (t instanceof ProvisionException)
    {
      throw (ProvisionException)t;
    }

    if (t instanceof Error)
    {
      throw (Error)t;
    }

    throw new ProvisionException(t.getMessage(), t);
  }

  /**
   * @author Eike Stepper
   */
  public static class Factory implements ITargetLocationFactory
  {
    private static final String LOCATION = "location";

    private static final String LOCATION_TYPE = "type";

    private static final String LOCATION_PROFILE = "profile";

    private static final String TARGLET = "targlet";

    private static final String TARGLET_NAME = "name";

    private static final String TARGLET_ACTIVE_REPOSITORY_LIST = "activeRepositoryList";

    private static final String ROOT = "root";

    private static final String ROOT_ID = "id";

    private static final String ROOT_VERSION_RANGE = "versionRange";

    private static final String SOURCE_LOCATOR = "sourceLocator";

    private static final String SOURCE_LOCATOR_ROOT_FOLDER = "rootFolder";

    private static final String SOURCE_LOCATOR_LOCATE_NESTED_PROJECTS = "locateNestedProjects";

    private static final String REPOSITORY_LIST = "repositoryList";

    private static final String REPOSITORY_LIST_NAME = "name";

    private static final String REPOSITORY = "repository";

    private static final String REPOSITORY_URL = "url";

    public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException
    {
      Element containerElement;

      try
      {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(serializedXML.getBytes("UTF-8")));
        containerElement = document.getDocumentElement();
      }
      catch (Exception e)
      {
        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
      }

      if (TYPE.equals(type) && containerElement != null)
      {
        String locationType = containerElement.getAttribute(LOCATION_TYPE);
        if (locationType.equals(type))
        {
          String profileID = containerElement.getAttribute(LOCATION_PROFILE);
          TargletBundleContainer container = new TargletBundleContainer(profileID);

          NodeList targletNodes = containerElement.getChildNodes();
          for (int i = 0; i < targletNodes.getLength(); i++)
          {
            Node targletNode = targletNodes.item(i);
            if (targletNode instanceof Element)
            {
              Element targletElement = (Element)targletNode;

              Targlet targlet = SetupFactory.eINSTANCE.createTarglet();
              targlet.setName(targletElement.getAttribute(TARGLET_NAME));
              targlet.setActiveRepositoryList(targletElement.getAttribute(TARGLET_NAME));
              container.addTarglet(targlet);

              NodeList childNodes = targletElement.getChildNodes();
              for (int j = 0; j < childNodes.getLength(); j++)
              {
                Node childNode = childNodes.item(j);
                if (childNode instanceof Element)
                {
                  Element childElement = (Element)childNode;
                  String tag = childElement.getTagName();
                  if (ROOT.equals(tag))
                  {
                    InstallableUnit root = SetupFactory.eINSTANCE.createInstallableUnit();
                    root.setID(childElement.getAttribute(ROOT_ID));
                    root.setVersionRange(new VersionRange(childElement.getAttribute(ROOT_VERSION_RANGE)));
                    targlet.getRoots().add(root);
                  }
                  else if (SOURCE_LOCATOR.equals(tag))
                  {
                    AutomaticSourceLocator sourceLocator = SetupFactory.eINSTANCE.createAutomaticSourceLocator();
                    sourceLocator.setRootFolder(childElement.getAttribute(SOURCE_LOCATOR_ROOT_FOLDER));
                    sourceLocator.setLocateNestedProjects(Boolean.valueOf(childElement
                        .getAttribute(SOURCE_LOCATOR_LOCATE_NESTED_PROJECTS)));
                    targlet.getSourceLocators().add(sourceLocator);
                  }
                  else if (REPOSITORY_LIST.equals(tag))
                  {
                    RepositoryList repositoryList = SetupFactory.eINSTANCE.createRepositoryList();
                    repositoryList.setName(childElement.getAttribute(REPOSITORY_LIST_NAME));

                    NodeList repositoryNodes = childElement.getChildNodes();
                    for (int k = 0; k < repositoryNodes.getLength(); k++)
                    {
                      Node repositoryNode = repositoryNodes.item(k);
                      if (repositoryNode instanceof Element)
                      {
                        Element repositoryElement = (Element)repositoryNode;

                        P2Repository p2Repository = SetupFactory.eINSTANCE.createP2Repository();
                        p2Repository.setURL(repositoryElement.getAttribute(REPOSITORY_URL));
                        repositoryList.getP2Repositories().add(p2Repository);
                      }
                    }
                  }
                }
              }
            }
          }

          // XMLResource resource = new XMLResourceImpl();
          //
          // try
          // {
          // resource.load(containerElement, null);
          //
          // for (EObject object : resource.getContents())
          // {
          // if (object instanceof Targlet)
          // {
          // Targlet targlet = (Targlet)object;
          // container.addTarglet(targlet);
          // }
          // }
          // }
          // catch (Exception ex)
          // {
          // Activator.log(ex);
          // }

          return container;
        }
      }

      return null;
    }

    public static String serialize(String profileID, List<Targlet> targlets)
    {
      Element containerElement;
      Document document;

      try
      {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = docBuilder.newDocument();
      }
      catch (Exception ex)
      {
        Activator.log(ex);
        return null;
      }

      containerElement = document.createElement(LOCATION);
      containerElement.setAttribute(LOCATION_TYPE, TYPE);
      containerElement.setAttribute(LOCATION_PROFILE, profileID);
      document.appendChild(containerElement);

      for (Targlet targlet : targlets)
      {
        Element targletElement = document.createElement(TARGLET);
        targletElement.setAttribute(TARGLET_NAME, targlet.getName());
        targletElement.setAttribute(TARGLET_ACTIVE_REPOSITORY_LIST, targlet.getActiveRepositoryList());
        containerElement.appendChild(targletElement);

        for (InstallableUnit root : targlet.getRoots())
        {
          Element rootElement = document.createElement(ROOT);
          rootElement.setAttribute(ROOT_ID, root.getID());
          rootElement.setAttribute(ROOT_VERSION_RANGE, root.getVersionRange().toString());
          targletElement.appendChild(rootElement);
        }

        for (AutomaticSourceLocator sourceLocator : targlet.getSourceLocators())
        {
          Element sourceLocatorElement = document.createElement(SOURCE_LOCATOR);
          sourceLocatorElement.setAttribute(SOURCE_LOCATOR_ROOT_FOLDER, sourceLocator.getRootFolder());
          sourceLocatorElement.setAttribute(SOURCE_LOCATOR_LOCATE_NESTED_PROJECTS,
              Boolean.toString(sourceLocator.isLocateNestedProjects()));
          targletElement.appendChild(sourceLocatorElement);
        }

        for (RepositoryList repositoryList : targlet.getRepositoryLists())
        {
          Element repositoryListElement = document.createElement(REPOSITORY_LIST);
          repositoryListElement.setAttribute(REPOSITORY_LIST_NAME, repositoryList.getName());
          targletElement.appendChild(repositoryListElement);

          for (P2Repository p2Repository : repositoryList.getP2Repositories())
          {
            Element p2RepositoryElement = document.createElement(REPOSITORY);
            p2RepositoryElement.setAttribute(REPOSITORY_URL, p2Repository.getURL());
            repositoryListElement.appendChild(p2RepositoryElement);
          }
        }
      }

      // XMLResource resource = new XMLResourceImpl();
      // resource.getContents().addAll(targlets);
      //
      // try
      // {
      // Map<Object, Object> options = new HashMap<Object, Object>();
      // // options.put(XMLResource.OPTION_DOM_USE_NAMESPACES_IN_SCOPE, false);
      // // options.put(XMLResource.OPTION_FORMATTED, true);
      //
      // Document emfDocument = resource.save(null, options, null);
      //
      // NodeList childNodes = emfDocument.getChildNodes();
      // for (int i = 0; i < childNodes.getLength(); i++)
      // {
      // Node item = childNodes.item(i);
      // if (item instanceof Element)
      // {
      // Element childElement = (Element)item.cloneNode(true);
      // document.adoptNode(childElement);
      // containerElement.appendChild(childElement);
      // }
      // }
      // }
      // finally
      // {
      // resource.getContents().clear();
      // }

      try
      {
        StreamResult result = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), result);

        String xml = result.getWriter().toString();
        // System.out.println(xml);
        return xml;
      }
      catch (TransformerException ex)
      {
        return null;
      }
    }
  }
}
