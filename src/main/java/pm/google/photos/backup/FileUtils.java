package pm.google.photos.backup;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FilenameUtils;


public class FileUtils {

	/**
	 * Deletes the file with the specified path from the specified root dir.
	 * All empty parent directories up to the root dir are also deleted.
	 *
	 * @param rootDir the directory against which the path is resolved
	 * @param pathToDelete a relative path specifying the file to delete
	 */
	public static boolean deleteQuietly(File rootDir, Path pathToDelete) {

		if (!rootDir.isDirectory()) {
			throw new IllegalArgumentException("rootDir is not a directory:" + rootDir);
		}

		if (pathToDelete.isAbsolute()) {
			throw new IllegalArgumentException("pathToDelete is not a relative path:" + pathToDelete);
		}

		// delete the file itself
		File fileToDelete = rootDir.toPath().resolve(pathToDelete).toFile();
		if (!fileToDelete.delete()) {
			return false;
		}

		// delete empty parent directories (up to the root directory)
		File parentDir = fileToDelete.getParentFile();
		while (!parentDir.equals(rootDir) && isEmptyDirectory(parentDir)) {
			parentDir.delete();
			parentDir = parentDir.getParentFile();
		}

		return true;
	}

	/**
	 * Checks if the specified file is an empty directory.
	 * @param dir
	 */
	public static boolean isEmptyDirectory(File dir) {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("directory does not exist or is a file: " + dir);
		}
		File[] files = dir.listFiles();
		return files == null || files.length == 0;
	}

	/**
	 * Creates any necessary but nonexistent parent directories for the specified file.
	 * @param file
	 */
	public static boolean createParentDirectories(File file) {
		return file.getParentFile().mkdirs();
	}

	/**
	 * Returns the first non existing file with the specified filename, by adding an index suffix
	 * between the base name and the file extension. For example, if the filename is "IMG_20201002_195004.jpg"
	 * the returned file might be "IMG_20201002_195004_001.jpg"
	 *
	 * @param parent
	 * @param filename
	 */
	public static File getNextFile(File parent, String filename) {

		String basename = FilenameUtils.getBaseName(filename);
		String extension = FilenameUtils.getExtension(filename);

		File file = null;
		int index = 1;

		do {
			filename = String.format("%s_%03d.%s", basename, index, extension);
			file = new File(parent, filename);
			index++;
		} while (file.exists());

		return file;
	}

	/**
	 * Returns the path of the specified file relative to the specified rootDir.
	 * @param rootDir
	 * @param file
	 */
	public static Path getRelativePath(File rootDir, File file) {
		Path rootPath = rootDir.toPath();
		Path filePath = file.toPath();
		return rootPath.relativize(filePath);
	}

	/**
	 * Downloads a file from the specified URL and writes it to the specified destination file.
	 * If the destination file already exists it will be overwritten.
	 *
	 * @param sourceUrl
	 * @param destinationFile
	 * @throws IOException
	 */
	public static void downloadFile(URL sourceUrl, File destinationFile) throws IOException {
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection)sourceUrl.openConnection();
			long fileSize = urlConnection.getContentLengthLong();
			createParentDirectories(destinationFile);
			download(urlConnection, destinationFile, fileSize);

		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}

	private static void download(URLConnection urlConnection, File destinationFile, long fileSize) throws IOException {

		final OpenOption[] OVERWRITE_IF_EXISTS = new OpenOption[] {
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

		try (ReadableByteChannel inChannel = Channels.newChannel(urlConnection.getInputStream());
				FileChannel outChannel = FileChannel.open(destinationFile.toPath(), OVERWRITE_IF_EXISTS)) {
			transfer(inChannel, outChannel, fileSize);
		}
	}

	private static void transfer(ReadableByteChannel in, FileChannel out, long size) throws IOException {
		long position = 0;
		while (position < size) {
			position += out.transferFrom(in, position, size);
		}
	}
}
