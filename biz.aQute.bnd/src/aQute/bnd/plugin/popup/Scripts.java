package aQute.bnd.plugin.popup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;

import aQute.bnd.build.Project;
import aQute.bnd.plugin.Activator;

public class Scripts extends CompoundContributionItem {
	final List<Project>	projects	= new ArrayList<Project>();

	public Scripts() {
		ISelectionService is = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService();
		ISelection s = is.getSelection();
		if (s != null && s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			for (Iterator<?> i = ss.iterator(); i.hasNext();) {
				Object oo = i.next();
				IJavaProject jp = null;
				if (oo instanceof IResource) {
					IResource r = (IResource) oo;
					IProject iproject = r.getProject();
					jp = JavaCore.create(iproject);
				} else if (oo instanceof IJavaProject) {
					jp = (IJavaProject) oo;
				}

				if (jp != null) {
					if (!jp.getProject().isAccessible()) {
						continue;
					}

					Project project = Activator.getDefault().getCentral().getModel(jp);
					if (project != null) {
						File bndFile = project.getFile("bnd.bnd");
						if (!bndFile.exists()) {
							continue;
						}
						projects.add(project);
					}
				}
			}
		}
	}

	public Scripts(String id) {
		super(id);
	}

	@Override protected IContributionItem[] getContributionItems() {
		if (projects.isEmpty())
			return new IContributionItem[0];

		Set<String> titles = new HashSet<String>();
		boolean first = true;
		for (Project project : projects) {
			if (first) {
				titles.addAll(project.getActions().keySet());
				first = false;
			} else {
				titles.retainAll(project.getActions().keySet());
			}
		}

		SubMenu root = new SubMenu("root");

		SubMenu sub = new SubMenu("Bnd");

		root.add(sub);

		for (final String title : titles) {
			sub.add(this, title, title);
		}

		return root.getItems();
	}

	void exec(final String label) {
		Job job = new Job(label) {
			protected IStatus run(IProgressMonitor monitor) {

				for (Project project : projects) {
					if (monitor != null) {
						if (monitor.isCanceled())
							break;
						monitor.subTask("" + project + " " + label);
					}

					Map<String, aQute.bnd.service.action.Action> actions = project.getActions();
					aQute.bnd.service.action.Action cmd = actions.get(label);
					try {
						project.action(label);
						monitor.worked(1);

						Activator.getDefault().getCentral().refresh(project);
						if (!project.isPerfect()) {

							// We had errors or warnings
							Activator.getDefault().report(true, true, project,
									"During execution of " + label, "");
							return Status.CANCEL_STATUS;
						}
					} catch (Throwable e) {
						Activator.getDefault().error("While executing action: " + cmd, e);
					}
				}
				try {
					Activator.getDefault().getCentral().refreshPlugins();
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to refresh: " + e);
				}
			}
		};
		job.setPriority(Job.SHORT);
		job.schedule(); // start as soon as possible
	}

}
