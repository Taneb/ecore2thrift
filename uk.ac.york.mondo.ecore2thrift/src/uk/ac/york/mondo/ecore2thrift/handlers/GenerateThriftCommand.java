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
import org.eclipse.epsilon.eol.models.ModelRepository;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class GenerateThriftCommand extends AbstractHandler implements IHandler {
	private static final String ECORE_URI = "http://www.eclipse.org/emf/2002/Ecore";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection0 = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection0 instanceof IStructuredSelection) {
			final IStructuredSelection selection = (IStructuredSelection)selection0; 
			final IFile ecore = (IFile)selection.getFirstElement();
			final File ecoreFile = ecore.getLocation().toFile();
			final EglFileGeneratingTemplateFactory factory = new EglFileGeneratingTemplateFactory();
	 		final IEolExecutableModule eglModule = new EglTemplateFactoryModuleAdapter(factory);
			final ModelRepository modelRepo = eglModule.getContext().getModelRepository();
			final EmfModel model = new EmfModel();
			model.setModelFile(ecoreFile.getAbsolutePath());
			model.setName(ecoreFile.getName());
			model.setMetamodelUri(ECORE_URI);
			try {
				model.load();
			} catch (EolModelLoadingException e) {
				e.printStackTrace();
				return null;
			}
			modelRepo.addModel(model);
			try {
				final URI ecore2thriftURI = GenerateThriftCommand.class.getResource("/egl/ecore2thrift.egl").toURI();
				final EglFileGeneratingTemplate template = (EglFileGeneratingTemplate)factory.load(ecore2thriftURI);
				template.process();
				for (StatusMessage message : factory.getContext().getStatusMessages())
					System.out.println(message);
				final File of = new File(ecoreFile.toString().substring(0, ecoreFile.toString().lastIndexOf('.')) + ".thrift");
				if (!of.createNewFile()) {
					// ask if the user wants to overwrite!
					final Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
					// if they say no, just return.
					if(!MessageDialog.open(MessageDialog.QUESTION, shell, "ecore2thrift", "Outpt file " + of.getName() + " already exists!\nDo you want to overwrite it?", SWT.NONE))
						return null;
				}
				template.generate("file://" + of.toString());
				ecore.getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
			} catch (EglRuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		// TODO Auto-generated method stub"
		return null;
	}

}
