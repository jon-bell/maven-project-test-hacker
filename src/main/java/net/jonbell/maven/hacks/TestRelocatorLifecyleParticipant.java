package net.jonbell.maven.hacks;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "project-sorter")
public class TestRelocatorLifecyleParticipant extends AbstractMavenLifecycleParticipant {

	static HashSet<String> pluginsToMigrate;
	static {
		pluginsToMigrate = new HashSet<String>();
		pluginsToMigrate.add("org.apache.maven.plugins:maven-surefire-plugin");
		pluginsToMigrate.add("org.apache.maven.plugins:maven-failsafe-plugin");
		pluginsToMigrate.add("org.apache.maven.plugins:maven-checkstyle-plugin");
		pluginsToMigrate.add("org.jacoco:jacoco-maven-plugin");
	}

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		LinkedList<MavenProject> newProjs = new LinkedList<MavenProject>();
		for (MavenProject proj : session.getProjects()) {
			HashSet<String> pluginsToMove = new HashSet<String>(pluginsToMigrate);
			pluginsToMove.retainAll(proj.getBuild().getPluginsAsMap().keySet());
			// System.out.println("TO move: " + pluginsToMove);
			if (pluginsToMove.size() > 0) {

				HashSet<Plugin> toMove = new HashSet<Plugin>();
				for (String plugToRemove : pluginsToMove) {
					Plugin t = proj.getPlugin(plugToRemove);
					toMove.add(t);
					proj.getBuildPlugins().remove(t);
				}
				MavenProject testProj = proj.clone();

				testProj.getBuild().getPlugins().clear();
				// testProj.setParent(proj);
				testProj.setArtifactId(proj.getArtifactId() + "-tests");
				testProj.setName(proj.getName() + " auto-split test module");
				testProj.setGroupId(proj.getGroupId());

				for (Plugin p : toMove)
					testProj.getBuild().addPlugin(p);

				//Adding a dep on the original module will only work if we do install first
				//But it shouldn't matter - if the module with tests runs first, it will handle the build steps?
//				Dependency depOnOriginalModule = new Dependency();
//				depOnOriginalModule.setArtifactId(proj.getArtifactId());
//				depOnOriginalModule.setGroupId(proj.getGroupId());
//				depOnOriginalModule.setVersion(proj.getVersion());
//				testProj.getDependencies().add(depOnOriginalModule);
				newProjs.add(testProj);
			}
		}
		session.getProjects().addAll(newProjs);
		super.afterProjectsRead(session);
	}
}