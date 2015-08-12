package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueue extends MeshVertex {

	public static final String SEARCH_QUEUE_ENTRY_ADDRESS = "search-queue-entry";

	/**
	 * Add another element to the search queue. Each element is identified by the uuid and type.
	 * 
	 * @param uuid
	 *            Uuid of the object that should be handled by the search index handler
	 * @param type
	 *            Type of the object that should be handled by the search index handler
	 * @param action
	 */
	void put(String uuid, String type, SearchQueueEntryAction action);

	void put(SearchQueueEntry entry);

	SearchQueueEntry take() throws InterruptedException;

	/**
	 * Returns the size of the queue.
	 * 
	 * @return
	 */
	long getSize();

}
