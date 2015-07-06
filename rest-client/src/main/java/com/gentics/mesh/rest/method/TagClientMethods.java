package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;

public interface TagClientMethods {

	Future<TagResponse> createTag(TagCreateRequest tagCreateRequest);

	Future<TagResponse> findTagByUuid(String projectName, String uuid);

	Future<TagResponse> updateTag(String projectName, String uuid, TagUpdateRequest tagUpdateRequest);

	Future<GenericMessageResponse> deleteTag(String uuid);

	Future<TagListResponse> findTags(String projectName, PagingInfo pagingInfo);

	//TODO keep this?
	Future<TagResponse> findTagByName(String name);

	Future<NodeListResponse> findNodesForTag(String tagUuid);

}
