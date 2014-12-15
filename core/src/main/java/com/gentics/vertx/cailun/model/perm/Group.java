package com.gentics.vertx.cailun.model.perm;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.vertx.cailun.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Group extends AbstractPersistable {

	private static final long serialVersionUID = -6423363555276535419L;

	private String name;

	boolean canView = true;

	boolean canUpdate = false;

	boolean canDelete = false;

	@Fetch
	@RelatedTo(type = "MEMBER_OF", direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> members = new HashSet<>();

	@Fetch
	@RelatedTo(type = "PARENT_OF", direction = Direction.OUTGOING, elementClass = Group.class)
	private Set<Group> parents = new HashSet<>();

	public Group(String name) {
		this.name = name;
	}
}
