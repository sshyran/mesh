package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class MicroschemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private MicroschemaCrudHandler crudHandler;

	protected MicroschemaVerticle() {
		super("microschemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());

		addDiffHandler();
		addChangesHandler();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

	}

	private void addDiffHandler() {
		Route route = route("/:uuid/diff").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("uuid");
			crudHandler.handleDiff(ac, schemaUuid);
		});
	}

	private void addChangesHandler() {
		Route getRoute = route("/:schemaUuid/changes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleGetSchemaChanges(ac, schemaUuid);
		});

		Route executeRoute = route("/:schemaUuid/changes").method(POST).produces(APPLICATION_JSON);
		executeRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = InternalActionContext.create(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});

	}

	private void addCreateHandler() {
		route().method(POST).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});

	}

}