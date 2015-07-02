package uk.ac.york.mondo.ecore2thrift;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Activator extends AbstractUIPlugin {
	private static Activator activator;

	public void logError(String msg, Exception e) {
		getLog().log(
				new Status(Status.INFO, "uk.ac.york.mondo.ecore2thrift",
						Status.ERROR, msg, e));
	}

	public Activator() {
		activator = this;
	}

	public static Activator getPlugin() {
		return activator;
	}

}
