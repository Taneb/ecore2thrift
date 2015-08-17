package uk.ac.york.mondo.ecore2thrift.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplate;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplateFactory;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.exceptions.EglRuntimeException;
import org.eclipse.epsilon.egl.status.StatusMessage;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.evl.EvlModule;
import org.eclipse.epsilon.evl.EvlUnsatisfiedConstraint;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.ac.york.mondo.ecore2thrift.Activator;

public class GenerateThriftCommand extends AbstractHandler implements IHandler {
	private static final String ECORE_URI = "http://www.eclipse.org/emf/2002/Ecore";
	
	private void addModelFromFile(IEolExecutableModule eglModule, File file) throws EolModelLoadingException{
		final EmfModel model = new EmfModel();
		model.setModelFile(file.getAbsolutePath());
		model.setName(file.getName());
		model.setMetamodelUri(ECORE_URI);
		model.load();
		eglModule.getContext().getModelRepository().addModel(model);
	}
	
	private void generateThriftFile(EglFileGeneratingTemplate template, File origin) throws IOException, EglRuntimeException {
		final File of = new File(origin.toString().substring(0, origin.toString().lastIndexOf('.')) + ".thrift");
		of.createNewFile(); // if we have to overwrite, just go for it
		template.generate(of.toURI().toString());
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Job job = new Job("ecore2thrift"){
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Generating Thrift file from Ecore", 3);
				
				final ISelection selection0 = HandlerUtil.getCurrentSelection(event);
				if (selection0 instanceof IStructuredSelection) {
					final IStructuredSelection selection = (IStructuredSelection)selection0; 
					final IFile ecore = (IFile)selection.getFirstElement();
					final File ecoreFile = ecore.getLocation().toFile();
					// run EVL script
					try {
						monitor.subTask("Validating");
						for (IMarker marker : ecore.findMarkers(EValidator.MARKER, false, IResource.DEPTH_INFINITE)) {
							if (marker.getAttribute("secondary-marker-type", "").equalsIgnoreCase("uk.ac.york.mondo.ecore2thift.validation")) {
								marker.delete();
							}
						}
						EvlModule validateModule = new EvlModule();
						addModelFromFile(validateModule, ecoreFile);
						validateModule.parse(GenerateThriftCommand.class.getResource("/epsilon/ecore2thrift.evl").toURI());
						validateModule.execute();
						
						List<EvlUnsatisfiedConstraint> unsatisfiedConstraints = validateModule.getContext().getUnsatisfiedConstraints();
						if (!unsatisfiedConstraints.isEmpty()) {
							boolean shouldStop = false;
							for (EvlUnsatisfiedConstraint unsatisfiedConstraint : unsatisfiedConstraints) {
								IMarker marker = ecore.createMarker(EValidator.MARKER);
								if (unsatisfiedConstraint.getConstraint().isCritique()) {
									marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
								}
								else {
									marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
									shouldStop = true;
								}
								marker.setAttribute(IMarker.MESSAGE, unsatisfiedConstraint.getMessage());
								marker.setAttribute("secondary-marker-type", "uk.ac.york.mondo.ecore2thift.validation");
							}
							if (shouldStop) return null; //TODO: show dialogue saying that there are problems					
						}
					} catch (Exception e) {
						Activator.getPlugin().logError("There was some error during validation.", e);
						return new Status(Status.ERROR, "ecore2thrift", "There was some error during validation", e);
					}
					monitor.worked(1);
					if (monitor.isCanceled()) {
						monitor.done();
						return Status.CANCEL_STATUS;						
					}
					final EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
					try {
						monitor.subTask("Loading model");
						final IEolExecutableModule eglModule = new EglTemplateFactoryModuleAdapter(factory);
						addModelFromFile(eglModule, ecoreFile);
					} catch (Exception e) {
						Activator.getPlugin().logError("There was an error while loading the model", e);
						return new Status(Status.ERROR, "ecore2thrift", "There was an error while loading the model", e);
					}
					monitor.worked(1);
					if (monitor.isCanceled()) {
						monitor.done();
						return Status.CANCEL_STATUS;
					}
					try {
						monitor.subTask("Processing model");
						final URI ecore2thriftURI = GenerateThriftCommand.class.getResource("/epsilon/ecore2thrift.egl").toURI(); // should I bother giving this a name?
						final EglFileGeneratingTemplate template = (EglFileGeneratingTemplate)factory.load(ecore2thriftURI);
						template.process();
						for (StatusMessage message : factory.getContext().getStatusMessages())
							System.out.println(message);
						generateThriftFile(template, ecoreFile);
					} catch (Exception e) {
						Activator.getPlugin().logError("There was some error while processing the model", e);
						return new Status(Status.ERROR, "ecore2thrift", "There was some error while processing the model", e);
					}
					monitor.worked(1);
					if (monitor.isCanceled()) {
						monitor.done();
						return Status.CANCEL_STATUS;
					}
					monitor.subTask("Refreshing project");
					try {
						ecore.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
					} catch (CoreException e) {
						// Not sure when this would be raised.
						// Something to do with situations when you can't refresh?
						Activator.getPlugin().logError("There was an error while refreshing the project", e);
						return new Status(Status.ERROR, "ecore2thrift", "There was an error while refreshing the project", e);
					}
					monitor.done();
					
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		
		return null;
	}

}
