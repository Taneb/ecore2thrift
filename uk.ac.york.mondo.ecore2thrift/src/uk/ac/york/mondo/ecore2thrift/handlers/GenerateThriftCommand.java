package uk.ac.york.mondo.ecore2thrift.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplate;
import org.eclipse.epsilon.egl.EglFileGeneratingTemplateFactory;
import org.eclipse.epsilon.egl.EglTemplateFactoryModuleAdapter;
import org.eclipse.epsilon.egl.exceptions.EglRuntimeException;
import org.eclipse.epsilon.egl.status.StatusMessage;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class GenerateThriftCommand extends AbstractHandler implements IHandler {
	private static final String ECORE_URI = "http://www.eclipse.org/emf/2002/Ecore";
	
	private void addModelToFactoryFromFile(EglFileGeneratingTemplateFactory factory, File file) throws EolModelLoadingException{
		final IEolExecutableModule eglModule = new EglTemplateFactoryModuleAdapter(factory);
		final EmfModel model = new EmfModel();
		model.setModelFile(file.getAbsolutePath());
		model.setName(file.getName());
		model.setMetamodelUri(ECORE_URI);
		model.load();
		eglModule.getContext().getModelRepository().addModel(model);
	}
	
	private void useTemplateToGenerateThriftFileWithFilenameBasedOnOriginFile(EglFileGeneratingTemplate template, File origin) throws IOException, EglRuntimeException {
		final File of = new File(origin.toString().substring(0, origin.toString().lastIndexOf('.')) + ".thift");
		of.createNewFile(); // if we have to overwrite, just go for it
		template.generate("file://" + of.toString());
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection0 = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection0 instanceof IStructuredSelection) {
			final IStructuredSelection selection = (IStructuredSelection)selection0; 
			final IFile ecore = (IFile)selection.getFirstElement();
			final File ecoreFile = ecore.getLocation().toFile();
			final EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
			try {
				addModelToFactoryFromFile(factory, ecoreFile);
			} catch (EolModelLoadingException e) {
				e.printStackTrace();
				return null;
			}
			try {
				final URI ecore2thriftURI = GenerateThriftCommand.class.getResource("/egl/ecore2thrift.egl").toURI(); // should I bother giving this a name?
				final EglFileGeneratingTemplate template = (EglFileGeneratingTemplate)factory.load(ecore2thriftURI);
				template.process();
				for (StatusMessage message : factory.getContext().getStatusMessages())
					System.out.println(message);
				useTemplateToGenerateThriftFileWithFilenameBasedOnOriginFile(template, ecoreFile);
			} catch (EglRuntimeException e) {
				// This is the exception to watch out for, I think
				// It should be raised on a malformed ecore file
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// Something has gone wrong with the ecore2thrift.egl file
				// I would be VERY surprised were this to happen
				e.printStackTrace();
			} catch (IOException e) {
				// This will be raised if something goes horribly wrong when creating the file.
				e.printStackTrace();
			} 
			try {
				ecore.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				// Not sure when this would be raised.
				// Something to do with situations when you can't refresh?
				e.printStackTrace();
			}
		}
		return null;
	}

}
