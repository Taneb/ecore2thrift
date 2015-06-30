package uk.ac.york.mondo.ecore2thrift.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
	
public class GenerateThriftCommand extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		MessageDialog.openInformation(shell, "Clicked", "Clicked!");
		// TODO Auto-generated method stub
		return null;
	}

}
