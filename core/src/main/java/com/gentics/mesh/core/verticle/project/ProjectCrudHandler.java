package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Handler for project specific requests.
 */
public class ProjectCrudHandler extends AbstractCrudHandler<Project, ProjectResponse> {

	private BootstrapInitializer boot;

	@Inject
	public ProjectCrudHandler(Database db, BootstrapInitializer boot) {
		super(db);
		this.boot = boot;
	}

	@Override
	public RootVertex<Project> getRootVertex(InternalActionContext ac) {
		return boot.projectRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

	/**
	 * Handle a read project by name request.
	 * 
	 * @param ac
	 * @param projectName
	 *            Name of the project which should be read.
	 */
	public void handleReadByName(InternalActionContext ac, String projectName) {
		Database db = MeshInternal.get().database();
		db.asyncNoTx(() -> {
			RootVertex<Project> root = getRootVertex(ac);
			Project project = root.findByName(ac, projectName, READ_PERM);
			return project.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
