package com.gentics.mesh.plugin;

import java.util.List;
import java.util.stream.Collectors;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class AuthPlugin extends AbstractPlugin implements AuthServicePlugin {
	public AuthPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		return new MappingResult()
			.setUser(extractUser(token))
			.setGroups(extractGroups(token))
			.setRoles(extractRoles(token));
	}

	private UserUpdateRequest extractUser(JsonObject token) {
		return new UserUpdateRequest()
			.setUsername(token.getString("preferred_username"))
			.setEmailAddress(token.getString("email"));
	}

	private List<GroupResponse> extractGroups(JsonObject token) {
		return token.getJsonArray("groups")
			.stream()
			.map(String.class::cast)
			.map(name -> new GroupResponse().setName(name))
			.collect(Collectors.toList());
	}

	private List<RoleResponse> extractRoles(JsonObject token) {
		return token.getJsonArray("roles")
			.stream()
			.map(String.class::cast)
			.map(name -> new RoleResponse().setName(name))
			.collect(Collectors.toList());
	}
}
