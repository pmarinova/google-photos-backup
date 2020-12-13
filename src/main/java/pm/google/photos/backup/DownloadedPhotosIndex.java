package pm.google.photos.backup;

import java.time.LocalDate;
import java.util.List;


public interface DownloadedPhotosIndex {

	DownloadedMediaItem getItem(String itemId);

	void addItem(DownloadedMediaItem item);

	void removeItem(DownloadedMediaItem item);

	/**
	 * Finds all downloaded items of the specified type for the specified date range (inclusive).
	 * @param type the media item type or null
	 * @param startDate the start date or null
	 * @param endDate the end date or null
	 * @return the list of items from the index matching the criteria
	 */
	List<DownloadedMediaItem> findItems(MediaItemType type, LocalDate startDate, LocalDate endDate);
}
