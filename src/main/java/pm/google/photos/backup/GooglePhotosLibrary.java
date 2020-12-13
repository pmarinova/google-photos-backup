package pm.google.photos.backup;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.google.auth.Credentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.DateFilter;
import com.google.photos.library.v1.proto.Filters;
import com.google.photos.library.v1.proto.MediaTypeFilter;
import com.google.photos.library.v1.proto.MediaTypeFilter.MediaType;
import com.google.photos.types.proto.DateRange;
import com.google.photos.types.proto.MediaItem;
import com.google.protobuf.Timestamp;
import com.google.type.Date;


/**
 * A wrapper for the google photos client -
 * {@linkplain PhotosLibraryClient}.
 */
public class GooglePhotosLibrary {

	private static final LocalDate DATE_MIN = LocalDate.of(1, 1, 1);
	private static final LocalDate DATE_MAX = LocalDate.of(9999, 12, 31);

	private final Credentials credentials;

	private PhotosLibraryClient client;

	public GooglePhotosLibrary(Credentials credentials) {
		this.credentials = credentials;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

	public void initialize() {
		try {
			PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
					.setCredentialsProvider(this::getCredentials).build();
			this.client = PhotosLibraryClient.initialize(settings);

		} catch (IOException ex) {
			throw new RuntimeException("Failed to initialize photos library", ex);
		}
	}

	public void close() {
		this.client.close();
	}

	public List<MediaItem> listMediaItems(MediaItemType mediaItemType, LocalDate startDate, LocalDate endDate) {

		MediaType mediaType = mediaItemType != null ?
				toMediaType(mediaItemType) : MediaType.ALL_MEDIA;

		DateRange dateRange = DateRange.newBuilder()
				.setStartDate(toDate(startDate != null ? startDate : DATE_MIN))
				.setEndDate(toDate(endDate != null ? endDate : DATE_MAX))
				.build();

		Filters filters = Filters.newBuilder()
				.setMediaTypeFilter(MediaTypeFilter.newBuilder().addMediaTypes(mediaType))
				.setDateFilter(DateFilter.newBuilder().addRanges(dateRange))
				.build();

		List<MediaItem> items = new ArrayList<>();
		this.client.searchMediaItems(filters).iterateAll().forEach(items::add);
		return items;
	}

	private static MediaType toMediaType(MediaItemType mediaItemType) {
		switch (mediaItemType) {
		case PHOTO: return MediaType.PHOTO;
		case VIDEO: return MediaType.VIDEO;
		default: return null;
		}
	}

	private static Date toDate(LocalDate localDate) {
		return Date.newBuilder()
				.setYear(localDate.getYear())
				.setMonth(localDate.getMonthValue())
				.setDay(localDate.getDayOfMonth())
				.build();
	}

	/**
	 * Returns the type of the specified media item.
	 * @param item
	 */
	public static MediaItemType getMediaItemType(MediaItem item) {
		if (item.getMediaMetadata().hasPhoto()) {
			return MediaItemType.PHOTO;
		} else if (item.getMediaMetadata().hasVideo()) {
			return MediaItemType.VIDEO;
		} else {
			return null;
		}
	}

	/**
	 * Returns the creation date of the specified media item as a local date.
	 * This is the date when the media item was first created, not the date
	 * when it was uploaded to Google Photos.
	 *
	 * Note that the result depends on the current system time-zone.
	 *
	 * @param item
	 */
	public static LocalDate getCreationDate(MediaItem item) {
		Timestamp creationTime = item.getMediaMetadata().getCreationTime();
		return Instant
				.ofEpochSecond(creationTime.getSeconds(), creationTime.getNanos())
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
	}

	/**
	 * Returns the download URL for the specified media item.
	 * Note that the download URL is valid only for a limited time (~ 1 hour).
	 * @see https://developers.google.com/photos/library/guides/access-media-items#image-base-urls
	 * @param item
	 */
	public static URL getDownloadURL(MediaItem item) {
		try {
			return new URL(item.getBaseUrl() + "=d");
		} catch (IOException ex) {
			throw new RuntimeException("Failed to get media item download url", ex);
		}
	}
}
