package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.node.AbstractBinaryVerticleTest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.query.impl.NodeRequestParameter.LinkType;
import com.gentics.mesh.util.FieldUtil;
import com.gentics.mesh.util.URIUtils;

import io.vertx.core.Future;

public class WebRootVerticleTest extends AbstractBinaryVerticleTest {

	@Autowired
	private WebRootVerticle webrootVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(webrootVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testReadBinaryNode() throws IOException {
		Node node = content("news_2015");

		// 1. Transform the node into a binary content
		SchemaContainer container = schemaContainer("binary-content");
		node.setSchemaContainer(container);
		node.getGraphFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
		prepareSchema(node, "image/*", "binary");
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		// 2. Update the binary data
		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "node_binary_field_updated", node.getUuid());

		// 3. Try to resolve the path
		String path = "/News/2015/somefile.dat";
		Future<WebRootResponse> webrootFuture = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setResolveLinks(LinkType.FULL));
		latchFor(webrootFuture);
		assertSuccess(webrootFuture);
		NodeDownloadResponse downloadResponse = webrootFuture.result().getDownloadResponse();
		assertTrue(webrootFuture.result().isBinary());
		assertNotNull(downloadResponse);

	}

	@Test
	public void testReadFolderByPath() throws Exception {
		Node folder = folder("2015");
		String path = "/News/2015";

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path);
		latchFor(future);
		assertSuccess(future);
		WebRootResponse restNode = future.result();
		test.assertMeshNode(folder, restNode.getNodeResponse());
		// assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
		// restNode.getProperties());
		// assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
		// restNode.getProperties());
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		Node content = content("news_2015");

		content.getGraphFieldContainer(english()).getHtml("content")
				.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");

		String path = "/News/2015/News_2015.en.html";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path,
				new NodeRequestParameter().setResolveLinks(LinkType.FULL).setLanguages("en"));
		latchFor(future);
		assertSuccess(future);
		WebRootResponse restNode = future.result();
		HtmlFieldImpl contentField = restNode.getNodeResponse().getFields().getHtmlField("content");
		assertNotNull(contentField);
		assertEquals("Check rendered content", "<a href=\"/api/v1/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>", contentField.getHTML());
		test.assertMeshNode(content, restNode.getNodeResponse());
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setLanguages("en", "de"));
		latchFor(future);
		assertSuccess(future);
		WebRootResponse restNode = future.result();

		Node node = content("news_2015");
		test.assertMeshNode(node, restNode.getNodeResponse());
		// assertNotNull(restNode.getProperties());

	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {

		Node parentNode = folder("2015");
		// Update content schema and add node field
		SchemaContainer folderSchema = schemaContainer("folder");
		Schema schema = folderSchema.getLatestVersion().getSchema();
		schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
		folderSchema.getLatestVersion().setSchema(schema);
		ServerSchemaStorage.getInstance().addSchema(schema);

		// Create content which is only german
		SchemaContainer contentSchema = schemaContainer("content");
		Node node = parentNode.create(user(), contentSchema.getLatestVersion(), project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(german(), contentSchema.getLatestVersion());
		englishContainer.createString("name").setString("german_name");
		englishContainer.createString("title").setString("german title");
		englishContainer.createString("displayName").setString("german displayName");
		englishContainer.createString("filename").setString("test.de.html");

		// Add node reference to node 2015
		parentNode.getGraphFieldContainer(english()).createNode("nodeRef", node);

		String path = "/News/2015";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path,
				new NodeRequestParameter().setResolveLinks(LinkType.MEDIUM).setLanguages("en", "de"));
		latchFor(future);
		assertSuccess(future);
		WebRootResponse restNode = future.result();
		assertEquals("The node reference did not point to the german node.", "/dummy/News/2015/test.de.html",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

		// Again with no german fallback option (only english)
		future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setResolveLinks(LinkType.MEDIUM).setLanguages("en"));
		latchFor(future);
		assertSuccess(future);
		restNode = future.result();
		assertEquals("The node reference did not point to the 404 path.", "/dummy/error/404",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		List<Future<WebRootResponse>> futures = new ArrayList<>();
		for (int i = 0; i < nJobs; i++) {
			futures.add(getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setLanguages("en", "de")));
		}

		for (Future<WebRootResponse> fut : futures) {
			latchFor(fut);
			assertSuccess(fut);
		}
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setLanguages("en", "de"));
		latchFor(future);
		assertSuccess(future);
	}

	@Test
	public void testPathWithPlus() throws Exception {
		//Test RFC3986 subdelims and an additional space and questionmark
		String newName = "20!$&'()*+,;=%3F? 15";
		String uuid = folder("2015").getUuid();
		Node folder = folder("2015");
		folder.getGraphFieldContainer("en").getString("name").setString(newName);

		String[] path = new String[] { "News", newName };
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path,
				new NodeRequestParameter().setLanguages("en", "de").setResolveLinks(LinkType.SHORT));
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertEquals(uuid, response.getNodeResponse().getUuid());
		assertEquals("/News/" + URIUtils.encodeFragment(newName), response.getNodeResponse().getPath());
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, path);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", "/blub");
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyPath() {
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, "");
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadProjectBaseNode() {
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, "/");
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertFalse(response.isBinary());
		assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadDoubleSlashes() {
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, "//");
		latchFor(future);
		WebRootResponse response = future.result();
		assertFalse(response.isBinary());
		assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g: /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					for (String language : Arrays.asList("en", "de")) {
						Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME,
								new String[] { path1, path2, path3, name + "." + language + ".html" });
						latchFor(future);
						assertSuccess(future);
						WebRootResponse response = future.result();

						assertEquals("Check response language", language, response.getNodeResponse().getLanguage());
					}
				}
			}
		}
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "/News/2015";
		Node newsFolder;
		newsFolder = folder("2015");
		role().revokePermissions(newsFolder, READ_PERM);

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, englishPath);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", newsFolder.getUuid());
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "/News/2015/no-valid-content.html";

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "/News/no-valid-folder/no-valid-content.html";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testRead404Page() {
		String notFoundPath = "/error/404";

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, notFoundPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", notFoundPath);
	}

	/**
	 * Test reading the "not found" path /error/404, when this resolves to an existing node. We expect the node to be returned, but the status code still to be
	 * 404
	 */
	@Test
	public void testRead404Node() {
		String notFoundPath = "/error/404";

		NodeCreateRequest createErrorFolder = new NodeCreateRequest();
		createErrorFolder.setSchema(new SchemaReference().setName("folder"));
		createErrorFolder.setParentNodeUuid(project().getBaseNode().getUuid());
		createErrorFolder.getFields().put("name", FieldUtil.createStringField("error"));
		createErrorFolder.setLanguage("en");
		Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, createErrorFolder);
		latchFor(future);
		assertSuccess(future);
		String errorNodeUuid = future.result().getUuid();

		NodeCreateRequest create404Node = new NodeCreateRequest();
		create404Node.setSchema(new SchemaReference().setName("content"));
		create404Node.setParentNodeUuid(errorNodeUuid);
		create404Node.getFields().put("filename", FieldUtil.createStringField("404"));
		create404Node.getFields().put("name", FieldUtil.createStringField("Error Content"));
		create404Node.getFields().put("content", FieldUtil.createStringField("An error happened"));
		create404Node.setLanguage("en");
		future = getClient().createNode(PROJECT_NAME, create404Node);
		latchFor(future);
		assertSuccess(future);

		Future<WebRootResponse> webrootFuture = getClient().webroot(PROJECT_NAME, notFoundPath);
		latchFor(webrootFuture);
		expectFailureMessage(webrootFuture, NOT_FOUND, null);
	}
}