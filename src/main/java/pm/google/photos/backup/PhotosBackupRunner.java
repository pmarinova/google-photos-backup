package pm.google.photos.backup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.photos.types.proto.MediaItem;


public class PhotosBackupRunner {

	private final GooglePhotosLibrary photosLibrary;
	private final DownloadedPhotosIndex photosIndex;
	private final File backupDir;

	private MediaItemType mediaItemType;
	private LocalDate startDate = null;
	private LocalDate endDate = null;


	public PhotosBackupRunner(GooglePhotosLibrary photosLibrary, DownloadedPhotosIndex photosIndex, File backupDir) {
		this.photosLibrary = photosLibrary;
		this.photosIndex = photosIndex;
		this.backupDir = backupDir;
	}

	public void setMediaItemType(MediaItemType mediaItemType) {
		this.mediaItemType = mediaItemType;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public void run() {

		System.out.println("Listing photos library items...");
		long startTime = System.nanoTime();

		Map<String, DownloadedMediaItem> downloadedItems =
				this.photosIndex.findItems(mediaItemType, startDate, endDate).stream()
				.collect(Collectors.toMap(DownloadedMediaItem::getId, Function.identity()));

		Map<String, MediaItem> libraryItems =
				this.photosLibrary.listMediaItems(mediaItemType, startDate, endDate).stream()
				.collect(Collectors.toMap(MediaItem::getId, Function.identity()));

		Set<String> removedItems = diff(downloadedItems.keySet(), libraryItems.keySet());
		Set<String> newItems = diff(libraryItems.keySet(), downloadedItems.keySet());

		System.out.printf("%d items to be removed.\n", removedItems.size());
		System.out.printf("%d new items found.\n", newItems.size());

		for (String removedItemId : removedItems) {
			removeItem(downloadedItems.get(removedItemId));
		}

		for (String newItemId : newItems) {
			backupItem(libraryItems.get(newItemId), newItems.size());
		}

		long stopTime = System.nanoTime();
		System.out.printf("Backup finshed in %d minutes.\n", TimeUnit.NANOSECONDS.toMinutes(stopTime-startTime));
	}

	private void removeItem(DownloadedMediaItem item) {
		FileUtils.deleteQuietly(this.backupDir, Paths.get(item.getFilePath()));
		this.photosIndex.removeItem(item);
		System.out.println("Removed " + item.getFilePath());
	}

	private void backupItem(MediaItem item, int itemsCount) {
		try {
			MediaItemType itemType = GooglePhotosLibrary.getMediaItemType(item);
			LocalDate creationDate = GooglePhotosLibrary.getCreationDate(item);
			URL downloadUrl = GooglePhotosLibrary.getDownloadURL(item);

			File destinationFile = getDestinationFile(creationDate, item.getFilename());
			System.out.println("Downloading " + destinationFile);
			FileUtils.downloadFile(downloadUrl, destinationFile);

			DownloadedMediaItem downloadedItem = new DownloadedMediaItem(item.getId());
			downloadedItem.setType(itemType);
			downloadedItem.setCreationDate(creationDate);
			downloadedItem.setFilePath(FileUtils.getRelativePath(this.backupDir, destinationFile).toString());
			this.photosIndex.addItem(downloadedItem);

		} catch (IOException ex) {
			throw new RuntimeException("Failed to backup item " + item);
		}
	}

	private File getDestinationFile(LocalDate creationDate, String filename) {
		File destinationDir = new File(this.backupDir, creationDate.getYear() + File.separator + creationDate.getMonthValue());
		File destinationFile = new File(destinationDir, filename);
		if (destinationFile.exists()) destinationFile = FileUtils.getNextFile(destinationDir, filename);
		return destinationFile;
	}

	/**
	 * Returns the difference between the two sets, i.e A-B.
	 * Returns a new set with all elements in A that are not in B.
	 */
	private static <T> Set<T> diff(Set<T> A, Set<T> B) {
		Set<T> result = new HashSet<>(A);
		result.removeAll(B);
		return result;
	}
}
