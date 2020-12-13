package pm.google.photos.backup;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jds.JDSTable;
import jds.JDataStorage;


/**
 * Data storage for the downloaded photos index.
 */
public class DownloadedPhotosJDS implements DownloadedPhotosIndex {

	private final JDataStorage jds;


	public static DownloadedPhotosJDS create(File storageDir) {
		JDataStorage jds = new JDataStorage()
				.homeFolder(storageDir.getPath())
				.with(
					new JDSTable<String, DownloadedMediaItem>("photos")
					.keyType(String.class)
					.valueType(DownloadedMediaItem.class)
					.keyProvider(DownloadedMediaItem::getId)
				);
		return new DownloadedPhotosJDS(jds);
	}

	private DownloadedPhotosJDS(JDataStorage jds) {
		this.jds = jds;
	}

	public void initialize() {
		this.jds.connect();
	}

	public void close() {
		this.jds.close();
	}

	@Override
	public DownloadedMediaItem getItem(String itemId) {
		return this.jds.transactionalFn(() ->
						jds.get(DownloadedMediaItem.class, itemId));
	}

	@Override
	public void addItem(DownloadedMediaItem item) {
		this.jds.transactional(() -> jds.insert(item));
	}

	@Override
	public void removeItem(DownloadedMediaItem item) {
		this.jds.transactional(() -> jds.delete(item));
	}

	@Override
	public List<DownloadedMediaItem> findItems(MediaItemType type, LocalDate startDate, LocalDate endDate) {

		List<DownloadedMediaItem> items =
				this.jds.transactionalFn(() ->
						jds.list(DownloadedMediaItem.class));

		return items.stream()
			.filter(item -> type != null ? item.getType() == type : true)
			.filter(item -> inDateRange(item.getCreationDate(), startDate, endDate))
			.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Checks if the specified date is in the specified [startDate:endDate] range.
     * <pre>
     *   LocalDate start = LocalDate.of(2020, 10, 30);
     *   LocalDate end   = LocalDate.of(2020, 11, 15);
     *
     *   inDateRange(LocalDate.of(2020, 10, 29), start, end) == false
     *   inDateRange(LocalDate.of(2020, 10, 31), start, end) == true
     *   inDateRange(LocalDate.of(2020, 10, 16), start, end) == false
     *
     *   inDateRange(start, start, end) == true
     *   inDateRange(end, start, end) == true
     * </pre>
     * </p>
	 */
	private boolean inDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
		if (startDate == null) startDate = LocalDate.MIN;
		if (endDate == null) endDate = LocalDate.MAX;
		return !date.isBefore(startDate) && !date.isAfter(endDate);
	}
}
