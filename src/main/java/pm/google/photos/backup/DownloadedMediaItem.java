package pm.google.photos.backup;

import java.io.Serializable;
import java.time.LocalDate;


public class DownloadedMediaItem implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The unique media item id assigned by Google Photos.
	 */
	private final String id;

	/**
	 * The media item type - photo or video.
	 */
	private MediaItemType type;

	/**
	 * The media item creation date.
	 * This is the date when the media item was first created, not when it was
	 * uploaded to Google Photos or downloaded by GooglePhotosBackup.
	 */
	private LocalDate creationDate;

	/**
	 * The path denoting the file to which the media item was downloaded.
	 * This is not absolute path, but a path relative to the backup directory.
	 */
	private String filePath;


	public DownloadedMediaItem(String mediaItemId) {
		this.id = mediaItemId;
	}

	public String getId() {
		return this.id;
	}

	public MediaItemType getType() {
		return this.type;
	}

	public void setType(MediaItemType type) {
		this.type = type;
	}

	public LocalDate getCreationDate() {
		return this.creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public String toString() {
		return getFilePath().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!obj.getClass().equals(getClass()))
            return false;
        DownloadedMediaItem other = (DownloadedMediaItem)obj;
        return this.id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
}
